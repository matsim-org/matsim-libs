// code by jph
// v2 by clruch
package playground.clruch.io.fleet;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.datalys.MultiFileReader;
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
        // TODO change this to generic input... rename in a way to show what it is.

        File directory = //

                new File("/home/andya/Desktop/idsc_st/10_Daten/2017-10-11_ZurichNew");
        ReferenceFrame referenceFrame = ReferenceFrame.SWITZERLAND;
        // File file = new File("/media/datahaki/media/ethz/taxi", "2017-06-27 - GPS Fahrtstrecken-Protokoll.csv");
        List<File> trailFiles = (new MultiFileReader(directory, "Fahrtstrecken")).getFolderFiles();
        System.out.println("found files: ");
        for (File file : trailFiles) {
            System.out.println(file.getAbsolutePath());
        }
        DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
        // extract data from file and put into dayTaxiRecord
        CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);
        
        reader.populateFrom(trailFiles);

        File simulationDirectory = new File(args[0]);
        Network network = NetworkLoader.loadNetwork(simulationDirectory);

        // STEP 2: DayTaxiRecord to MATSimStaticDatabase
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        // generate sim objects and store
        File storageSupplierFile = new File(simulationDirectory.getParent(), "output");
        System.out.println("supplierFile: " + storageSupplierFile.getAbsolutePath());

//        if (storageSupplierFile.exists()) {
//            FileDelete.of(storageSupplierFile, 5, 100000);
//        }
//        GlobalAssert.that(!storageSupplierFile.exists());
//        storageSupplierFile.mkdir();
        System.out.println("supplierFile: " + storageSupplierFile.getAbsolutePath());

        StorageUtils storageUtils = new StorageUtils(storageSupplierFile);
        SimulationFleetDump.of(dayTaxiRecord, network, MatsimStaticDatabase.INSTANCE, storageUtils);
    }

}




        // File file = new File("/media/datahaki/media/ethz/taxi", "2017-06-27 - GPS
        //List<File> trailFiles = (new MultiFileReader(directory, "Fahrtstrecken")).getFolderFiles();















