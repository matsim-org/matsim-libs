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
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.options.ScenarioOptions;
import playground.clruch.utils.NetworkLoader;

/** @author Claudio Ruch */
enum StandaloneFleetConverter {
    ;
    public static void main(String[] args) throws Exception {
        File workingDirectory = MultiFileTools.getWorkingDirectory();
        ScenarioOptions simOptions = ScenarioOptions.load(workingDirectory);

        // File config = new File(workingDirectory, simOptions.getString("simuConfig"));
        // Config config = ConfigUtils.loadConfig(simOptions.getSimulationConfigName());

        File networkFile = new File(workingDirectory, simOptions.getString("simuConfig"));
        Network network = NetworkLoader.loadNetwork(networkFile);
        // File networkFile = new File(workingDirectory, "trb_config.xml");

        System.out.println("INFO working folder: " + workingDirectory.getAbsolutePath());
        System.out.println("INFO network file: " + networkFile.getAbsolutePath());
        ReferenceFrame referenceFrame = ReferenceFrame.SWITZERLAND;
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
        for (File file : trailFiles) {
            System.out.println(file.getAbsolutePath());

            DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
            // extract data from file and put into dayTaxiRecord
            CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);
            reader.populateFrom(file, true);
            dayTaxiRecords.add(dayTaxiRecord);

            outputDirectory = new File(workingDirectory, "output/" + file.getName().substring(0, 10));
            if (outputDirectory.exists() == false)
                outputDirectory.mkdir();
            GlobalAssert.that(outputDirectory.isDirectory());
            outputFolders.add(outputDirectory);
            System.out.println("INFO output Folder: " + outputDirectory.getAbsolutePath());
        }

        // STEP 2: DayTaxiRecord to MATSimStaticDatabase
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);

        // generate sim objects and store
        SimulationFleetDump.of(dayTaxiRecords, network, MatsimStaticDatabase.INSTANCE, outputFolders);
    }
}
