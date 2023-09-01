package com.chiral.examples;

import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.chiral.client.Client;
import com.chiral.client.ClientBuilder;
import com.chiral.client.apps.ChiralException;

import java.io.IOException;
import java.nio.file.Files;

public class RecgenTest {

    @Test
    public void testReCGen()
    {
        try {
            String userEmail = System.getenv("CHIRAL_USER_EMAIL");
            String apiToken = System.getenv("CHIRAL_TOKEN_API");
            Client client = ClientBuilder.build(userEmail, apiToken);
            com.chiral.client.apps.chem.recgen.build.JobManager jm = new com.chiral.client.apps.chem.recgen.build.JobManager("DrugBank_M.db");

            String[] samples = { "sample4", "sample1" };
            int[] result_counts = { 33, 1117 };

            for (int i = 0; i < samples.length; i++) {
                String mol_file = String.format("./files/recgen/%s.mol", samples[i]);
                List<String> lines = Files.readAllLines(Paths.get(mol_file));
                String mol = String.join("\n", lines);
                String jobId = jm.submitJob(client, mol);
                client.waitUntilCompletion(jobId);
                ArrayList<String> smiles = jm.getOutput(client, jobId);
                assertTrue(smiles.size() == result_counts[i]);
            }
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
