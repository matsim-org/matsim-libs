// code by jph
// v2 by clruch
package playground.clruch.io.fleet;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.datalys.MultiFileReader;
import ch.ethz.idsc.queuey.datalys.MultiFileTools;
import ch.ethz.idsc.queuey.util.GlobalAssert;

import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.net.StorageUtils;
import playground.clruch.utils.NetworkLoader;

/** @author Claudio Ruch */
enum StandaloneFleetConverter {
    ;
    public static void main(String[] args) throws Exception {
        // STEP 1: File to DayTaxiRecord
        // selection of reference frame, file
        File directory = //
                new File("/home/anape/Downloads/SanFranciscoTaxi/data");
        GlobalAssert.that(directory.exists());
        GlobalAssert.that(directory.isDirectory());

        List<File> trailFiles = new MultiFileReader(directory).getFolderFiles();

        for (File file : trailFiles) {
            System.out.println("loaded file: " + file.getAbsolutePath());
        }

        File workingDirectory = MultiFileTools.getWorkingDirectory();
        File outputDirectory = new File(workingDirectory, "output");

        StorageUtils storageUtils = new StorageUtils(outputDirectory);
        ReferenceFrame referenceFrame = ReferenceFrame.SWITZERLAND;

        DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
        
        // File file = new File("/media/datahaki/media/ethz/taxi", "2017-06-27 - GPS
        //List<File> trailFiles = (new MultiFileReader(directory, "Fahrtstrecken")).getFolderFiles();
        
        // extract data from file and put into dayTaxiRecord
        CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);
        reader.populateFrom(trailFiles);
        
        // =================================================
        // TODO from here onwards you will need a San Francisco network... 
        Network network = NetworkLoader.loadNetwork(new File(args[0]));

        // STEP 2: DayTaxiRecord to MATSimStaticDatabase
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        // generate sim objects and store
        SimulationFleetDump.of(dayTaxiRecord, network, MatsimStaticDatabase.INSTANCE, storageUtils);

    }

}
