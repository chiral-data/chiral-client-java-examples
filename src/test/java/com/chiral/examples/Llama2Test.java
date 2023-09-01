package com.chiral.examples;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.chiral.client.Client;
import com.chiral.client.ClientBuilder;
import com.chiral.client.apps.ChiralException;
import com.chiral.client.apps.ml.llama2.Output;

import java.io.IOException;

public class Llama2Test {

    @Test
    public void testLlama2()
    {
        try {
            String userEmail = System.getenv("CHIRAL_USER_EMAIL");
            String apiToken = System.getenv("CHIRAL_TOKEN_API");
            Client client = ClientBuilder.build(userEmail, apiToken);
            com.chiral.client.apps.ml.llama2.JobManager jm = new com.chiral.client.apps.ml.llama2.JobManager();
            String jobId = jm.submitJob(client, 0.0, "Emma likes dogs and cats");
            client.waitUntilCompletion(jobId);
            Output output = jm.getOutput(client, jobId);
            // System.out.println(output.text);
            assertTrue(output.text.length() > 0); 
            client.close();
        } catch (ChiralException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (InterruptedException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
