# Java Vaulted Tokenization with ALTR

This project is a library for performing Tokenization and Detokenization with ALTR's Vaulted Tokenization API.

## Usage

The `com.altr.vault.ALTRTokenize` class can be initialized using the following parameters:

- `mapiKey`: Your ALTR Management API Key
- `mapiSecret`: Your ALTR Management API Secret
- `url`: The url endpoint for Vaulted Tokenization. This can be constructed as follows: `"https://<ALTR_ORG_ID>.vault.live.altr.com/api/v2/batch"`

Tokenization and Detokenization can be accomplished via the following methods of the `ALTRTokenize` class:
    
- `public List<String> detokenizeDeterministic(List<String> tokens)`
- `public List<String> detokenizeNonDeterministic(List<String> tokens)`
- `public List<String> tokenizeDeterministic(List<String> tokens)`
- `public List<String> tokenizeNonDeterministic(List<String> tokens)`
- `public Map<String, String> detokenizeDeterministic(Map<String, String> tokenMap)`
- `public Map<String, String> detokenizeNonDeterministic(Map<String, String> tokenMap)`
- `public Map<String, String> tokenizeDeterministic(Map<String, String> tokenMap)`
- `public Map<String, String> tokenizeNonDeterministic(Map<String, String> tokenMap)`

Each of the Tokenization/Detokenization throws the following exceptions:

- `com.altr.exception.ALTRException` - General exception
- `com.altr.exception.RetriesExhaustedException` - Extends `ALTRException`; Tokenization has failed and no retries are remaninig
- `com.altr.exception.BadRequestException` - Extends `ALTRException`; A Bad Request was sent to the Vaulted Tokenization API
- `com.altr.exception.UnauthorizedException` - Extends `ALTRException`; The credentials provided are not authorized to perform Tokenization
- `com.altr.exception.InternalErrorException` - Extends `ALTRException`; An error internal to ALTR has occurred. Please reach out to support@altr.com

## Testing

An Integration Test which uses ALTR's Vaulted Tokenization API directly can be performed by running `./.integration-test.sh`, after filling in the appropriate environment variables.

## Documentation:
ALTR Vaulted Tokenization: https://docs.altr.com/en/data-tokenization.html