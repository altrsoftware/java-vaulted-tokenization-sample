package com.altr;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.altr.exception.ALTRException;
import com.altr.vault.ALTRTokenize;

public class IntegrationTest {

    static String sampleToken;
    static String samplePlaintext = "john.doe@example.com";
    static String URL_TEMPLATE = "https://%s.vault.live.altr.com/api/v2/batch";

    public static void main(String[] args) throws ALTRException {
        String key = System.getenv("ALTR_KEY");
        String secret = System.getenv("ALTR_SECRET");
        String orgId = System.getenv("ALTR_ORG_ID");

        String url = String.format(URL_TEMPLATE, orgId);
        System.out.println("Using URL: " + url);
        System.out.println("Using Key: " + key);
        System.out.println("Using Secret: " + secret);
        ALTRTokenize altr = new ALTRTokenize(key, secret, url);

        /**********************
        Testing Tokenize List
        ***********************/
        System.out.println("\nTesting Tokenize List:");
        List<String> plaintextList = new ArrayList<String>();
        plaintextList.add(samplePlaintext);
        List<String> tokenizeListResponse = altr.tokenizeDeterministic(plaintextList);
        for (int i = 0; i < plaintextList.size(); i++) {
            String originalPlaintext = plaintextList.get(i);
            String tokenizedValue = tokenizeListResponse.get(i);
            System.out.println("Plaintext: " + originalPlaintext + " => Tokenized Value: " + tokenizedValue);
            IntegrationTest.sampleToken = tokenizedValue;
        }

        /**********************
        Testing Detokenize List
        ***********************/
        System.out.println("Testing Detokenize List:");
        List<String> tokens = new ArrayList<String>();
        tokens.add(sampleToken);
        List<String> detokenizeListResponse = altr.detokenizeDeterministic(tokens);
        for(int i = 0; i < tokens.size(); i++) {
            String originalToken = tokens.get(i);
            String detokenizedValue = detokenizeListResponse.get(i);
            System.out.println("Token: " + originalToken + " => Detokenized Value: " + detokenizedValue);
        }

        /**********************
        Testing Detokenize Map
        ***********************/
        System.out.println("\nTesting Detokenize Map:");
        Map<String, String> tokenMap = new HashMap<String, String>();
        tokenMap.put("1", sampleToken);
        Map<String, String> detokenizeMapResponseMap = altr.detokenizeDeterministic(tokenMap);
        for(Map.Entry<String, String> entry : detokenizeMapResponseMap.entrySet()) {
            String originalToken = tokenMap.get(entry.getKey());
            String detokenizedValue = entry.getValue();
            System.out.println("Token: " + originalToken + " => Detokenized Value: " + detokenizedValue);
        }

        /**********************
        Testing Tokenize Map
        ***********************/
        Date startTime = new Date();
        System.out.println("\nTesting Tokenize Map:");
        Map<String, String> plaintextMap = new HashMap<String, String>();
        for(int i = 0; i < 2000; i++) {
            plaintextMap.put(String.valueOf(i), samplePlaintext + i);
        }
        Map<String, String> tokenizeMapResponse = altr.tokenizeDeterministic(plaintextMap);
        Date endTime = new Date();
        for(Map.Entry<String, String> entry : tokenizeMapResponse.entrySet()) {
            String originalPlaintext = plaintextMap.get(entry.getKey());
            String tokenizedValue = entry.getValue();
            System.out.println("Plaintext: " + originalPlaintext + " => Tokenized Value: " + tokenizedValue);
        }
        System.out.println("Time taken for Tokenize Map: " + (endTime.getTime() - startTime.getTime()) + " ms");
    }
}
