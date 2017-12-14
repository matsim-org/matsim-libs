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
import playground.clruch.ScenarioPreparer;
import playground.clruch.ScenarioServer;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.options.ScenarioOptions;
import playground.clruch.utils.NetworkLoader;

/** @author Claudio Ruch */
enum StandaloneFleetConverter {
    ;
    
    private static int TIME_STEP = 10; // TODO Magic const. <=> Simulation resolution
    
    public static void main(String[] args) throws Exception {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        ScenarioOptions simOptions = ScenarioOptions.load(workingDirectory);

        // File config = new File(workingDirectory, simOptions.getString("simuConfig"));
        // Config config = ConfigUtils.loadConfig(simOptions.getSimulationConfigName());

        File configFile = new File(workingDirectory, simOptions.getSimulationConfigName());
        Network network = NetworkLoader.loadNetwork(configFile);
        
        // File networkFile = new File(workingDirectory, "trb_config.xml");

        System.out.println("INFO working folder: " + workingDirectory.getAbsolutePath());
        System.out.println("INFO network file: " + configFile.getAbsolutePath());
        ReferenceFrame referenceFrame = ReferenceFrame.SWITZERLAND;
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        
        List<File> trailFiles = (new MultiFileReader(workingDirectory, "Fahrtstrecken")).getFolderFiles();
        List<DayTaxiRecord> dayTaxiRecords = new ArrayList<>();
        List<File> outputFolders = new ArrayList<>();
        File outputDirectory = new File(workingDirectory, "output/");

        // File outputSubDirectory = new File(config.controler().getOutputDirectory());
        // File outputDirectory = outputSubDirectory.getParentFile();

        System.err.println("WARN All files in the following folder will be deleted. Hit ENTER key to continue:");
        System.err.println(outputDirectory.getAbsolutePath());

        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (workingDirectory.exists()) {
            if (outputDirectory.exists())
                FileDelete.of(outputDirectory, 5, 90000);
            outputDirectory.mkdir();
        }

        // Generate dayTaxiRecords and output folders according to date of csv-data
        System.out.println("INFO found files: ");
        boolean takeLog = false;
        for (File file : trailFiles) {
            System.out.println(file.getAbsolutePath());

            DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
            // extract data from file and put into dayTaxiRecord
            CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);
            reader.populateFrom(file, takeLog);
            FleetUtils.postProcessData(file, dayTaxiRecord, takeLog, TIME_STEP);
            FleetUtils.getLinkData(dayTaxiRecord, network, MatsimStaticDatabase.INSTANCE);
            dayTaxiRecords.add(dayTaxiRecord);

            outputDirectory = new File(workingDirectory, "output/" + file.getName().substring(0, 10));
            if (outputDirectory.exists() == false)
                outputDirectory.mkdir();
            GlobalAssert.that(outputDirectory.isDirectory());
            outputFolders.add(outputDirectory);
            System.out.println("INFO output Folder: " + outputDirectory.getAbsolutePath());
        }

        // STEP 2: DayTaxiRecord to MATSimStaticDatabase
        // generate sim objects and store
        SimulationFleetDump.of(dayTaxiRecords, network, MatsimStaticDatabase.INSTANCE, outputFolders, TIME_STEP);
        
        // STEP 3: Generate Population.xml
        outputDirectory = new File(workingDirectory, "output/");
        PopulationCreator.createAdamAndEva(workingDirectory, outputDirectory, network, MatsimStaticDatabase.INSTANCE);

        // STEP 4: Run ScenarioPreparer
        ScenarioPreparer scenarioPreparer = new ScenarioPreparer();
        scenarioPreparer.run(workingDirectory);

        // STEP 5: Run ScenarioServer
        ScenarioServer scenarioServer = new ScenarioServer();
        scenarioServer.main(null);
    }
}
