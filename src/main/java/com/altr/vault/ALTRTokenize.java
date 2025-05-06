package com.altr.vault;

import java.lang.Math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Base64;
import java.util.Map;
import java.util.List;
import java.io.IOException;

import com.altr.exception.ALTRException;
import com.altr.exception.BadRequestException;
import com.altr.exception.InternalErrorException;
import com.altr.exception.RateLimitExceededException;
import com.altr.exception.RetriesExhaustedException;
import com.altr.exception.UnauthorizedException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ALTR class provides methods to interact with ALTR's Vaulted Tokenization API
 */
public class ALTRTokenize {

    private static final int TOKEN_BATCH_SIZE = 1024;

    private static final String X_ALTR_DETERMINISM_HEADER = "X-ALTR-DETERMINISM";
    private static final String IS_DETERMINISTIC = "true";
    private static final String IS_NOT_DETERMINISTIC = "false";

    private static final String HTTP_READ_METHOD = "PUT";
    private static final String HTTP_WRITE_METHOD = "POST";

    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_INTERNAL_ERROR = 500;
    private static final int HTTP_RATE_LIMIT_EXCEEDED = 429;

    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;

    private final Request.Builder requestTemplate;
    
    /**
     * Constructor for ALTRTokenize class.
     * @param mapiKey ALTR Management API Key
     * @param mapiSecret ALTR Management API Secret
     * @param url URL of the ALTR Vaulted Tokenization API. Example: "https://<ALTR_ORG_ID>.vault.live.altr.com/api/v2/batch"
     */
    public ALTRTokenize(String mapiKey, String mapiSecret, String url) {
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((mapiKey + ":" + mapiSecret).getBytes());
        this.requestTemplate = new Request.Builder()
                .url(url)
                .addHeader("AUTHORIZATION", basicAuth)
                .addHeader("Content-Type", "application/json");
    }

    /**
     * Partitions a map into smaller maps of a specified chunk size.
     * @param <K> Type of keys in the map
     * @param <V> Type of values in the map
     * @param original The original map to be partitioned
     * @param chunkSize The size of each partition
     * @return ArrayList of maps, each containing a partition of the original map
     */
    private static <K, V> ArrayList<Map<K, V>> partitionMap(Map<K, V> original, int chunkSize) {
        ArrayList<Map<K, V>> partitions = new ArrayList<Map<K, V>>();
        Map<K, V> currentPartition = new HashMap<K, V>();

        int count = 0;
        for (Map.Entry<K, V> entry : original.entrySet()) {
            currentPartition.put(entry.getKey(), entry.getValue());
            count++;
            if (count == chunkSize) {
                partitions.add(currentPartition);
                currentPartition = new HashMap<>();
                count = 0;
            }
        }

        if (!currentPartition.isEmpty()) {
            partitions.add(currentPartition);
        }

        return partitions;
    }

    /**
     * Tokenizes or detokenizes batches of tokens.
     * @param batches List of maps representing batches of tokens to be tokenized or detokenized
     * @param isDetokenize boolean indicating whether to tokenize or detokenize
     * @param isDeterministic  boolean indicating whether the operation should be deterministic
     * @return Map<String, String> containing the results of the tokenization or detokenization
     * @throws ALTRException
     */
    private Map<String, String> tokenizeBatches(List<Map<String, String>> batches, boolean isDetokenize,
            boolean isDeterministic) throws ALTRException {
        Map<String, String> detokens = new HashMap<String, String>();
        try {
            for (Map<String, String> batch : batches) {
                if (batch.size() == 0)
                    continue;
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(batch);
                Map<String, String> currTokens = this.sendTokenizationRequest(json, isDetokenize, isDeterministic);
                detokens.putAll(currTokens);
            }
        } catch (JsonProcessingException e) {
            throw new ALTRException("Error while processing JSON: " + e.getMessage());
        }
        return detokens;
    }

    /**
     * Calculates the sleep time based on the retry count.
     * @param retryCount The number of retries that have been attempted.
     * @return int The calculated sleep time in milliseconds.
     */
    private int sleepTime(int retryCount) {
        return ALTRTokenize.BASE_SLEEP_TIME + (int) Math.pow(retryCount, 2) * ALTRTokenize.BASE_SLEEP_TIME;
    }

    /**
     * Determines whether a request should be retried based on the status code and retry count.
     * @param statusCode The HTTP status code returned from the request.
     * @param retryCount The number of retries that have been attempted.
     * @return boolean indicating whether the request should be retried.
     * @throws ALTRException
     */
    private boolean shouldRetry(int statusCode, int retryCount) throws ALTRException {
        switch (statusCode) {
            case HTTP_BAD_REQUEST:
                throw new BadRequestException();
            case HTTP_UNAUTHORIZED:
                throw new UnauthorizedException();
            case HTTP_INTERNAL_ERROR:
                throw new InternalErrorException();
            case HTTP_RATE_LIMIT_EXCEEDED:
                throw retryCount < ALTRTokenize.MAX_RETRIES ? new RateLimitExceededException() : new RetriesExhaustedException();
            default:
                throw new ALTRException("Request failed with status code: " + statusCode);
        }
    }

    /**
     * Sends a tokenization or detokenization request to the ALTR API.
     * @param json The JSON string representing the tokens to be tokenized or detokenized.
     * @param isDetokenize boolean indicating whether to tokenize or detokenize.
     * @param isDeterministic boolean indicating whether the operation should be deterministic.
     * @return Map<String, String> containing the results of the tokenization or detokenization.
     * @throws ALTRException
     */
    private Map<String, String> sendTokenizationRequest(String json, boolean isDetokenize, boolean isDeterministic) throws ALTRException {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(json, mediaType);
        Request request = requestTemplate
                .method(isDetokenize ? HTTP_READ_METHOD : HTTP_WRITE_METHOD, body)
                .addHeader(X_ALTR_DETERMINISM_HEADER, isDeterministic ? IS_DETERMINISTIC : IS_NOT_DETERMINISTIC)
                .build();
        Response response;
        Map<String, String> responseMap = new HashMap<>();
        int retryCount = 0;
        while(retryCount <= ALTRTokenize.MAX_RETRIES) {
            try {
                response = client.newCall(request).execute();
                int statusCode = response.code();
                if (statusCode != HTTP_OK) {
                    try {
                        shouldRetry(statusCode, retryCount);
                    } catch (RateLimitExceededException e) {
                        try {
                            Thread.sleep(sleepTime(retryCount));
                        } catch (InterruptedException ee) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    retryCount++;
                    continue;
                }
                JsonFactory jf = new JsonFactory();
                JsonParser jp = jf.createParser(response.body().string());
                ObjectMapper mapper = new ObjectMapper();
                Object parsedResponseBody = mapper.readValue(jp, HashMap.class);
                @SuppressWarnings("unchecked")
                HashMap<String, Map<String, String>> responseBody = (HashMap<String, Map<String, String>>) parsedResponseBody;
                responseMap = responseBody.containsKey("data") ? responseBody.get("data") : null;
                return responseMap;
            } catch (IOException e) {
                throw new ALTRException("Error while sending request: " + e.getMessage());
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Tokenizes or detokenizes a list of tokens.
     * @param tokens List of tokens to be tokenized or detokenized
     * @param isDetokenize boolean indicating whether to tokenize or detokenize
     * @param isDeterministic boolean indicating whether the operation should be deterministic
     * @return List<String> containing the transformed tokens
     * @throws ALTRException
     */
    private List<String> tokenize(List<String> tokens, boolean isDetokenize, boolean isDeterministic) throws ALTRException {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> tokenMap = new HashMap<>();
        for (String token : tokens) {
            tokenMap.put(token, token);
        }

        Map<String, String> responseMap = this.tokenize(tokenMap, isDetokenize, isDeterministic);

        List<String> transformedTokens = new ArrayList<String>();

        tokens.forEach(token -> {
            String transformed = responseMap.get(token);
            transformedTokens.add(transformed != null ? transformed : token);
        });

        return transformedTokens;
    }

    /**
     * Tokenizes or detokenizes a map of tokens.
     * @param tokenMap Map<String, String> containing tokens to be tokenized or detokenized
     * @param isDetokenize boolean indicating whether to tokenize or detokenize
     * @param isDeterministic boolean indicating whether the operation should be deterministic
     * @return Map<String, String> containing the transformed tokens
     * @throws ALTRException
     */
    private Map<String, String> tokenize(Map<String, String> tokenMap, boolean isDetokenize, boolean isDeterministic) throws ALTRException {
        if (tokenMap == null || tokenMap.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Map<String, String>> batches = ALTRTokenize.partitionMap(tokenMap, ALTRTokenize.TOKEN_BATCH_SIZE);

        return this.tokenizeBatches(batches, isDetokenize, isDeterministic);
    }

    /**
     * Detokenizes a list of tokens deterministically.
     * @param tokens List<String> of tokens to be detokenized
     * @return List<String> containing the detokenized tokens
     * @throws ALTRException
     * @throws RetriesExhaustedException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws InternalErrorException
     */
    public List<String> detokenizeDeterministic(List<String> tokens) throws ALTRException, RetriesExhaustedException, BadRequestException, UnauthorizedException, InternalErrorException {
        return this.tokenize(tokens, true, true);
    }

    /**
     * Detokenizes a map of tokens deterministically.
     * @param tokenMap Map<String, String> of tokens to be detokenized
     * @return Map<String, String> containing the detokenized tokens
     * @throws ALTRException
     * @throws RetriesExhaustedException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws InternalErrorException
     */
    public Map<String, String> detokenizeDeterministic(Map<String, String> tokenMap) throws ALTRException, RetriesExhaustedException, BadRequestException, UnauthorizedException, InternalErrorException {
        return this.tokenize(tokenMap, true, true);
    }

    /**
     * Detokenizes a list of tokens non-deterministically.
     * @param tokens List<String> of tokens to be detokenized
     * @return List<String> containing the detokenized tokens
     * @throws ALTRException
     * @throws RetriesExhaustedException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws InternalErrorException
     */
    public List<String> detokenizeNonDeterministic(List<String> tokens) throws ALTRException, RetriesExhaustedException, BadRequestException, UnauthorizedException, InternalErrorException {
        return this.tokenize(tokens, true, false);
    }

    /**
     * Detokenizes a map of tokens non-deterministically.
     * @param tokenMap Map<String, String> of tokens to be detokenized
     * @return Map<String, String> containing the detokenized tokens
     * @throws ALTRException
     * @throws RetriesExhaustedException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws InternalErrorException
     */
    public Map<String, String> detokenizeNonDeterministic(Map<String, String> tokenMap) throws ALTRException, RetriesExhaustedException, BadRequestException, UnauthorizedException, InternalErrorException {
        return this.tokenize(tokenMap, true, false);
    }

    /**
     * Tokenizes a list of tokens deterministically.
     * @param tokens List<String> of tokens to be tokenized
     * @return List<String> containing the tokenized tokens
     * @throws ALTRException
     * @throws RetriesExhaustedException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws InternalErrorException
     */
    public List<String> tokenizeDeterministic(List<String> tokens) throws ALTRException, RetriesExhaustedException, BadRequestException, UnauthorizedException, InternalErrorException {
        return this.tokenize(tokens, false, true);
    }

    /**
     * Tokenizes a map of tokens deterministically.
     * @param tokenMap Map<String, String> of tokens to be tokenized
     * @return Map<String, String> containing the tokenized tokens
     * @throws ALTRException
     * @throws RetriesExhaustedException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws InternalErrorException
     */
    public Map<String, String> tokenizeDeterministic(Map<String, String> tokenMap) throws ALTRException, RetriesExhaustedException, BadRequestException, UnauthorizedException, InternalErrorException {
        return this.tokenize(tokenMap, false, true);
    }

    /**
     * Tokenizes a list of tokens non-deterministically.
     * @param tokens List<String> of tokens to be tokenized
     * @return List<String> containing the tokenized tokens
     * @throws ALTRException
     * @throws RetriesExhaustedException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws InternalErrorException
     */
    public List<String> tokenizeNonDeterministic(List<String> tokens) throws ALTRException, RetriesExhaustedException, BadRequestException, UnauthorizedException, InternalErrorException {
        return this.tokenize(tokens, false, false);
    }

    /**
     * Tokenizes a map of tokens non-deterministically.
     * @param tokenMap Map<String, String> of tokens to be tokenized
     * @return Map<String, String> containing the tokenized tokens
     * @throws ALTRException
     * @throws RetriesExhaustedException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws InternalErrorException
     */
    public Map<String, String> tokenizeNonDeterministic(Map<String, String> tokenMap) throws ALTRException, RetriesExhaustedException, BadRequestException, UnauthorizedException, InternalErrorException {
        return this.tokenize(tokenMap, false, false);
    }

}
