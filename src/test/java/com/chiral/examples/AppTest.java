package com.chiral.examples;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.io.File;

import com.chiral.client.Client;
import com.chiral.client.apps.chem.gromacs.gmx_command.JobManager;
import com.chiral.client.apps.chem.gromacs.gmx_command.Output;

public class AppTest 
{
    private Client clientForTesting() {
        String serverAddr = System.getenv("CHIRAL_SERVER_ADDR");
        int serverPort = Integer.parseInt(System.getenv("CHIRAL_SERVER_PORT"));
        int ftpPort = Integer.parseInt(System.getenv("CHIRAL_FILE_SERVER_PORT"));
        String userId = System.getenv("CHIRAL_USER_ID");
        String apiToken = System.getenv("CHIRAL_API_TOKEN");
        return new Client(serverAddr, serverPort, serverAddr, ftpPort, userId, apiToken);
    }

    @Test
    public void testGromacs()
    {
        String simulationId = "test_simulation";
        String localDirectory = "files/lysozyme";
        Client client = clientForTesting(); 
        JobManager jm = new JobManager(simulationId, localDirectory);
        try {
            jm.clearFiles(client);
            // Job - prepare the topology
            {
                String[] filesUpload = { "1AKI_clean.pdb" };
                jm.uploadFiles(client, filesUpload);
                String[] filesInput = {"1AKI_clean.pdb"} ;
                String[] filesOutput = {"1AKI_processed.gro", "topol.top", "posre.itp"};
                String jobId = jm.submitJob(client, "pdb2gmx", "-f 1AKI_clean.pdb -o 1AKI_processed.gro -water spce", "15 0", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - define unit cell 
            {
                String[] filesInput = {"1AKI_processed.gro"} ;
                String[] filesOutput = {"1AKI_newbox.gro"};
                String jobId = jm.submitJob(client, "editconf", "-f 1AKI_processed.gro -o 1AKI_newbox.gro -c -d 1.0 -bt cubic", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - add solvent 
            {
                String[] filesInput = {"1AKI_newbox.gro", "topol.top"} ;
                String[] filesOutput = {"1AKI_solv.gro", "topol.top"};
                String jobId = jm.submitJob(client, "solvate", "-cp 1AKI_newbox.gro -cs spc216.gro -o 1AKI_solv.gro -p topol.top", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - add ions
            {
                String[] filesUpload = { "ions.mdp" };
                jm.uploadFiles(client, filesUpload);
                String[] filesInput = {"1AKI_solv.gro", "topol.top", "ions.mdp"} ;
                String[] filesOutput = {"1AKI_solv.gro", "topol.top", "ions.tpr"};
                String jobId = jm.submitJob(client, "grompp", "-f ions.mdp -c 1AKI_solv.gro -p topol.top -o ions.tpr", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
            }
            // Job - generate topology 
            {
                String[] filesInput = {"ions.tpr", "topol.top"} ;
                String[] filesOutput = {"1AKI_solv_ions.gro", "topol.top"};
                String jobId = jm.submitJob(client, "genion", "-s ions.tpr -o 1AKI_solv_ions.gro -p topol.top -pname NA -nname CL -neutral", "13", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
            }
            // Job - topology for Energy Minimization 
            {
                String[] filesUpload = { "minim.mdp" };
                jm.uploadFiles(client, filesUpload);
                String[] filesInput = {"minim.mdp", "1AKI_solv_ions.gro", "topol.top"} ;
                String[] filesOutput = {"em.tpr"};
                String jobId = jm.submitJob(client, "grompp", "-f minim.mdp -c 1AKI_solv_ions.gro -p topol.top -o em.tpr", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - run Energy Minimization 
            {
                System.out.println("Energy minimization mdrun");
                String[] filesInput = {"em.tpr"};
                String[] filesOutput = {"em.log", "em.edr", "em.trr", "em.gro"};
                String jobId = jm.submitJob(client, "mdrun", "-deffnm em", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId); 
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
                System.out.println("Energy minimization mdrun ... DONE");
            }
            // Job - Energy analysis 
            {
                String[] filesInput = {"em.edr"};
                String[] filesOutput = {"potential.xvg"};
                String jobId = jm.submitJob(client, "energy", "-f em.edr -o potential.xvg", "10 0", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId); 
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - nvt topology 
            {
                String[] filesUpload = { "nvt.mdp" };
                jm.uploadFiles(client, filesUpload);
                String[] filesInput = {"nvt.mdp", "em.gro", "topol.top", "posre.itp"} ;
                String[] filesOutput = {"nvt.tpr"};
                String jobId = jm.submitJob(client, "grompp", "-f nvt.mdp -c em.gro -r em.gro -p topol.top -o nvt.tpr", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - nvt mdrun
            {
                System.out.println("NVT mdrun ... START");
                String[] filesInput = {"nvt.tpr"};
                String[] filesOutput = {"nvt.edr", "nvt.log", "nvt.trr", "nvt.gro", "nvt.cpt"};
                String jobId = jm.submitJob(client, "mdrun", "-deffnm nvt", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
                System.out.println("NVT mdrun ... DONE");
            }
            // Job - nvt analysis
            {
                String[] filesInput = {"nvt.edr"};
                String[] filesOutput = {"temperature.xvg"};
                String jobId = jm.submitJob(client, "energy", "-f nvt.edr -o temperature.xvg", "16 0", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - npt topology 
            {
                String[] filesUpload = { "npt.mdp" };
                jm.uploadFiles(client, filesUpload);
                String[] filesInput = {"npt.mdp", "nvt.gro", "topol.top", "posre.itp", "nvt.cpt"} ;
                String[] filesOutput = {"npt.tpr"};
                String jobId = jm.submitJob(client, "grompp", "-f npt.mdp -c nvt.gro -r nvt.gro -p topol.top -t nvt.cpt -o npt.tpr", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - npt mdrun
            {
                System.out.println("NPT mdrun ... START");
                String[] filesInput = {"npt.tpr"};
                String[] filesOutput = {"npt.edr", "npt.log", "npt.trr", "npt.gro", "npt.cpt"};
                String jobId = jm.submitJob(client, "mdrun", "-deffnm npt", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
                System.out.println("NPT mdrun ... START");
            }
            // Job - npt analysis pressure
            {
                String[] filesInput = {"npt.edr"};
                String[] filesOutput = {"pressure.xvg"};
                String jobId = jm.submitJob(client, "energy", "-f npt.edr -o pressure.xvg", "18 0", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - npt analysis density 
            {
                String[] filesInput = {"npt.edr"};
                String[] filesOutput = {"density.xvg"};
                String jobId = jm.submitJob(client, "energy", "-f npt.edr -o density.xvg", "24 0", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - production topology 
            {
                String[] filesUpload = { "md.mdp" };
                jm.uploadFiles(client, filesUpload);
                String[] filesInput = {"md.mdp", "npt.gro", "topol.top", "npt.cpt"} ;
                String[] filesOutput = {"md_0_1.tpr"};
                String jobId = jm.submitJob(client, "grompp", "-f md.mdp -c npt.gro -p topol.top -t npt.cpt -o md_0_1.tpr", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                if (!output.success) {
                    System.out.println("Run job stdout: " + output.stdout);
                    System.out.println("Run job error: " + output.stderr);
                }
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - production mdrun
            {
                System.out.println("Production mdrun ... START");
                String[] filesInput = {"md_0_1.tpr"};
                String[] filesOutput = {"md_0_1.xtc", "md_0_1.log"};
                String jobId = jm.submitJob(client, "mdrun", "-deffnm md_0_1 -nb gpu", "", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
                System.out.println("Production mdrun ... START");
            }
            // Job - trjconv 
            {
                String[] filesInput = {"md_0_1.tpr", "md_0_1.xtc"};
                String[] filesOutput = {"md_0_1_noPBC.xtc"};
                String jobId = jm.submitJob(client, "trjconv", "-s md_0_1.tpr -f md_0_1.xtc -o md_0_1_noPBC.xtc -pbc mol -center", "1 0", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
            // Job - rms 
            {
                String[] filesInput = {"md_0_1.tpr", "md_0_1_noPBC.xtc"};
                String[] filesOutput = {"rmsd.xvg"};
                String jobId = jm.submitJob(client, "rms", "-s md_0_1.tpr -f md_0_1_noPBC.xtc -o rmsd.xvg -tu ns", "4 4", filesInput, filesOutput);
                jm.waitUntilCompletion(client, jobId);
                Output output = jm.getOutput(client, jobId);
                assertTrue(output.success);
                for (String file : filesOutput) { assertTrue(client.isRemoteFileExist(file, jm.getRemoteDirectory())); }
                jm.downloadFiles(client, filesOutput);
                for (String file : filesOutput) { 
                    File localFile = new File(localDirectory + "/" + file);
                    assertTrue(localFile.isFile());
                    localFile.delete();
                }
            }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        } catch (InterruptedException ex) {
            System.out.println("Error: " + ex.getMessage());
        }

        client.close();
    }

}
