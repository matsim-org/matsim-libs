// code by jph
// v2 by clruch
package playground.clruch.io.fleet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.datalys.MultiFileReader;
import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.FileDelete;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.utils.NetworkLoader;
import playground.clruch.utils.PropertiesExt;

/** @author Claudio Ruch */
enum StandaloneFleetConverter {
    ;
    public static void main(String[] args) throws Exception {
        // STEP 1: File to DayTaxiRecord
        // selection of reference frame, file
        // TODO change this to generic input... rename in a way to show what it is.

        // File directory = //
        // new File("/home/andya/Desktop/idsc_st/10_Daten/2017-10-11_ZurichNew");

        File workingDirectory = MultiFileTools.getWorkingDirectory();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        File configFile = new File(workingDirectory, simOptions.getString("simuConfig"));
        GlobalAssert.that(configFile.exists());
        Network network = NetworkLoader.loadNetwork(configFile);
        GlobalAssert.that(network!=null);
        System.out.println("network has " + network.getNodes().size() + " nodes");        

        

        File simulationDirectory = new File(args[0]);
        File networkFile = new File(simulationDirectory, "trb_config.xml");
        System.out.println("INFO working folder: " + simulationDirectory.getAbsolutePath());
        System.out.println("INFO network file: " + networkFile.getAbsolutePath());
        ReferenceFrame referenceFrame = ReferenceFrame.SWITZERLAND;
        List<File> trailFiles = (new MultiFileReader(simulationDirectory, "Fahrtstrecken")).getFolderFiles();
        List<DayTaxiRecord> dayTaxiRecords = new ArrayList<>();
        List<File> outputFolders = new ArrayList<>();
        File outputFolder = new File(simulationDirectory, "output/");
        
        System.err.println("WARN All files in the following folder will be deleted. Hit ENTER key to continue:");
        System.err.println(outputFolder.getAbsolutePath());
        
        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (simulationDirectory.exists()) {
             FileDelete.of(outputFolder, 5, 90000);
             outputFolder.mkdir();
        }
        
        // Generate dayTaxiRecords and output folders according to date of csv-data
        System.out.println("INFO found files: ");
        for (File file : trailFiles) {
            System.out.println(file.getAbsolutePath());
            
            DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
            // extract data from file and put into dayTaxiRecord
            CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);
       //     reader.populateFrom(file);
            dayTaxiRecords.add(dayTaxiRecord);
            
            outputFolder = new File(simulationDirectory, "output/" + file.getName().substring(0, 10));
            if (outputFolder.exists() == false)
                outputFolder.mkdir();
            GlobalAssert.that(outputFolder.isDirectory());
            outputFolders.add(outputFolder);
            System.out.println("INFO output Folder: " + outputFolder.getAbsolutePath());
        }
        
        // STEP 2: DayTaxiRecord to MATSimStaticDatabase
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);


        // generate sim objects and store
//        SimulationFleetDump.of(dayTaxiRecords, network, MatsimStaticDatabase.INSTANCE, outputFolders);
        }
}

        // File file = new File("/media/datahaki/media/ethz/taxi", "2017-06-27 - GPS
        //List<File> trailFiles = (new MultiFileReader(directory, "Fahrtstrecken")).getFolderFiles();
