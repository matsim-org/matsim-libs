/**
 * @author Claudio Ruch
 *
 */

package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.datalys.MultiFileReader;
import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.ScenarioOptions;
import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.IdIntegerDatabase;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.StorageUtils;
import playground.clruch.utils.NetworkLoader;
import playground.clruch.utils.PropertiesExt;

/** @author Claudio Ruch */
enum StandaloneFleetConverterSF {
    ;
    public static void main(String[] args) throws Exception {

        File workingDirectory = MultiFileTools.getWorkingDirectory();
        PropertiesExt simOptions = PropertiesExt.wrap(ScenarioOptions.load(workingDirectory));
        File configFile = new File(workingDirectory, simOptions.getString("simuConfig"));
        GlobalAssert.that(configFile.exists());
        Network network = NetworkLoader.loadNetwork(configFile);
        GlobalAssert.that(network != null);
        System.out.println("network has " + network.getNodes().size() + " nodes");
        File outputDirectory = new File(workingDirectory, "output/001");
        File dataDirectory = new File(workingDirectory, "taxiTraces");

        System.out.println("INFO working folder: " + workingDirectory.getAbsolutePath());
        System.out.println("INFO data folder: " + dataDirectory.getAbsolutePath());
        System.out.println("INFO output folder: " + outputDirectory.getAbsolutePath());

        // File shortTraceData = new File(dataDirectory, "new_abboip_short.txt");
        // GlobalAssert.that(shortTraceData.exists());
        // List<File> trailFiles = new ArrayList<>();
        // trailFiles.add(shortTraceData);

        // fillHeaders(dataDirectory);

        List<File> trailFilesComplete = (new MultiFileReader(dataDirectory, "new_")).getFolderFiles();
        System.out.println("NUMBER of data files = " + trailFilesComplete.size());
        System.out.println("INFO found files: ");
        for (File file : trailFilesComplete) {
            System.out.println(file.getAbsolutePath());
        }
        
        List<File> trailFiles = new ArrayList<>();
        for(int i = 0; i < 30; ++i){
            trailFiles.add(trailFilesComplete.get(i*5));
        }
        trailFiles.add(trailFilesComplete.get(1));
        trailFiles.add(trailFilesComplete.get(100));
        

        // TODO preprocess the taxi files and add one line with headers to each file

        ReferenceFrame referenceFrame = ReferenceFrame.IDENTITY;
        
        IdIntegerDatabase vehicleIdIntegerDatabase = new IdIntegerDatabase();        
        DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
        // extract data from file and put into dayTaxiRecord
        CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);
        reader.populateFrom(trailFiles);

        // // STEP 2: DayTaxiRecord to MATSimStaticDatabase
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        // generate sim objects and store

        // TODO ana include this if you want to delete the old versions, but be careful, it can delete everything!!!
        //// if (storageSupplierFile.exists()) {
        //// FileDelete.of(storageSupplierFile, 5, 100000);
        //// }
        //// GlobalAssert.that(!storageSupplierFile.exists());
        //// storageSupplierFile.mkdir();

        StorageUtils storageUtils = new StorageUtils(outputDirectory);
        SimulationFleetDump.of(dayTaxiRecord, network, MatsimStaticDatabase.INSTANCE, storageUtils);
    }

    private static void fillHeaders(File dataDirectory) throws IOException {
        String realHeader = "LATITUDE LONGITUDE OCCUPANCY TIME"; // TODO remove magic const.
        List<File> trailFiles = (new MultiFileReader(dataDirectory, "new_")).getFolderFiles();
        for (File file : trailFiles) {

            // check if header is there
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line1 = br.readLine();
            if (!line1.equals(realHeader)) {
                File headeredFile = new File(file.getParentFile(), "HEADERED" + file.getName());
                BufferedWriter writer = new BufferedWriter(new FileWriter(headeredFile));
                writer.write(realHeader);
                writer.write("\n");

                while (true) {
                    String line = br.readLine();
                    if (Objects.isNull(line))
                        break;
                    writer.write(line);
                    writer.write("\n");

                }
            }

        }
    }
}

// ===========================
// for later usage
// ============================
