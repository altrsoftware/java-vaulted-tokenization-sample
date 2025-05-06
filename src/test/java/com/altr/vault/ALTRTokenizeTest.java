package com.altr.vault;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import com.altr.exception.ALTRException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class ALTRTokenizeTest {

    private ALTRTokenize altrTokenize;
    private MockWebServer mockWebServer;

    @Before
    public void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
    
        // Update ALTRTokenize to use the mock server's URL
        String mockBaseUrl = mockWebServer.url("/").toString();
    
        String key = System.getenv("ALTR_KEY");
        String secret = System.getenv("ALTR_SECRET");

        this.altrTokenize = new ALTRTokenize(key, secret, mockBaseUrl);
    }

    @Test
    public void testTokenizeDeterministic() {
        this.mockWebServer.enqueue(new MockResponse()
            .setBody("{ \"data\": {\"sampleData1\": \"tokenizedData2\"} }")
            .setResponseCode(200));
        ArrayList<String> sampleData = new ArrayList<>();
        sampleData.add("sampleData1");
        try {
            List<String> actualOutput = this.altrTokenize.tokenizeDeterministic(sampleData);
            ArrayList<String> expectedOutput = new ArrayList<>();
            expectedOutput.add("tokenizedData2");
            Assert.assertEquals(expectedOutput, actualOutput);
        } catch (ALTRException e) {
            // this block should not be reached
            Assert.fail("Tokenization failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testDetokenizeDeterministic() {
        this.mockWebServer.enqueue(new MockResponse()
            .setBody("{ \"data\": {\"tokenizedData2\": \"sampleData1\"} }")
            .setResponseCode(200));
        ArrayList<String> sampleData = new ArrayList<>();
        sampleData.add("tokenizedData2");
        try {
            List<String> actualOutput = this.altrTokenize.detokenizeDeterministic(sampleData);
            ArrayList<String> expectedOutput = new ArrayList<>();
            expectedOutput.add("sampleData1");
            Assert.assertEquals(expectedOutput, actualOutput);
        } catch (ALTRException e) {
            // this block should not be reached
            Assert.fail("Detokenization failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testTokenizeNonDeterministic() {
        this.mockWebServer.enqueue(new MockResponse()
            .setBody("{ \"data\": {\"sampleData1\": \"tokenizedData2\"} }")
            .setResponseCode(200));
        ArrayList<String> sampleData = new ArrayList<>();
        sampleData.add("sampleData1");
        try {
            List<String> actualOutput = this.altrTokenize.tokenizeDeterministic(sampleData);
            ArrayList<String> expectedOutput = new ArrayList<>();
            expectedOutput.add("tokenizedData2");
            Assert.assertEquals(expectedOutput, actualOutput);
        } catch (ALTRException e) {
            // this block should not be reached
            Assert.fail("Tokenization failed with exception: " + e.getMessage());
        }
    }

    @Test
    public void testDetokenizeNonDeterministic() {
        this.mockWebServer.enqueue(new MockResponse()
            .setBody("{ \"data\": {\"tokenizedData2\": \"sampleData1\"} }")
            .setResponseCode(200));
        ArrayList<String> sampleData = new ArrayList<>();
        sampleData.add("tokenizedData2");
        try {
            List<String> actualOutput = this.altrTokenize.detokenizeNonDeterministic(sampleData);
            ArrayList<String> expectedOutput = new ArrayList<>();
            expectedOutput.add("sampleData1");
            Assert.assertEquals(expectedOutput, actualOutput);
        } catch (ALTRException e) {
            // this block should not be reached
            Assert.fail("Detokenization failed with exception: " + e.getMessage());
        }
    }

    @After
    public void tearDown() throws IOException {
        if (this.mockWebServer != null) {
            this.mockWebServer.shutdown();
        }
    }

}
