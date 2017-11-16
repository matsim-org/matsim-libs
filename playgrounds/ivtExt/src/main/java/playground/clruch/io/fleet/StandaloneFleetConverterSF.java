/**
 * @author Claudio Ruch
 *
 */

package playground.clruch.io.fleet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.datalys.MultiFileReader;
import ch.ethz.idsc.queuey.datalys.MultiFileTools;
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
        File headerDirectory = new File(dataDirectory, "HEADER");
        File idDirectory = new File(dataDirectory, "_cabs.txt");

        System.out.println("INFO working folder: " + workingDirectory.getAbsolutePath());
        System.out.println("INFO data folder: " + dataDirectory.getAbsolutePath());
        System.out.println("INFO output folder: " + outputDirectory.getAbsolutePath());

        List<File> trailFilesComplete = (new MultiFileReader(dataDirectory, "new_")).getFolderFiles();
        System.out.println("NUMBER of data files = " + trailFilesComplete.size());
        System.out.println("INFO found files: ");

        List<File> trailFiles = new ArrayList<>();
        int num = 0;


        // ID & HEADERfor Taxis
        ChangeDataSF.head(trailFilesComplete, headerDirectory, trailFiles);
        ArrayList<String> people, number;
        people = ChangeDataSF.name(idDirectory);
        number = ChangeDataSF.id(idDirectory);

        // extract data from file and put into dayTaxiRecord
        DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
        CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);

        reader.populateFrom(trailFilesComplete, people, num);
        ReferenceFrame referenceFrame = ReferenceFrame.IDENTITY;
        IdIntegerDatabase vehicleIdIntegerDatabase = new IdIntegerDatabase();
        // STEP 2: DayTaxiRecord to MATSimStaticDatabase
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        // generate sim objects and store

        // TODO ana include this if you want to delete the old versions, but be careful, it can delete everything!!!
        // if (storageSupplierFile.exists()) {
        // FileDelete.of(storageSupplierFile, 5, 100000);
        // }
        // GlobalAssert.that(!storageSupplierFile.exists());
        // storageSupplierFile.mkdir();

        StorageUtils storageUtils = new StorageUtils(outputDirectory);
        SimulationFleetDump.of(dayTaxiRecord, network, MatsimStaticDatabase.INSTANCE, storageUtils);
        num++;

    }
}