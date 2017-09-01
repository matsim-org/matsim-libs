// code by jph
// v2 by clruch
package playground.clruch.io.fleet;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.network.Network;

import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.utils.NetworkLoader;

/** @author Claudio Ruch */
enum StandaloneFleetConverter {
    ;
    public static void main(String[] args) throws Exception {
        // STEP 1: File to DayTaxiRecord
        // selection of reference frame, file
        File directory = //
                new File("/home/clruch/Downloads/2017-06-29 - ETH GPS Protokolle & Auftragslisten");
        ReferenceFrame referenceFrame = ReferenceFrame.SWITZERLAND;
        //File file = new File("/media/datahaki/media/ethz/taxi", "2017-06-27 - GPS Fahrtstrecken-Protokoll.csv");
        List<File> trailFiles = (new ZHFileReader(directory, "Fahrtstrecken")).getTrailFiles();
        DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
        // extract data from file and put into dayTaxiRecord
        CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);
        reader.populateFrom(trailFiles);
        Network network = NetworkLoader.loadNetwork(new File(args[0]));

        // STEP 2: DayTaxiRecord to MATSimStaticDatabase
        MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
        // generate sim objects and store
        SimulationFleetDump.of(dayTaxiRecord, network, MatsimStaticDatabase.INSTANCE);
        
    }

}
