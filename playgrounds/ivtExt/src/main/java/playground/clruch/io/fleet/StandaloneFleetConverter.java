// code by jph
package playground.clruch.io.fleet;

import java.io.File;

import org.matsim.api.core.v01.network.Network;

import playground.clruch.data.ReferenceFrame;
import playground.clruch.net.MatsimStaticDatabase;
import playground.clruch.utils.NetworkLoader;

enum StandaloneFleetConverter {
	;
	public static void main(String[] args) throws Exception {
		ReferenceFrame referenceFrame = ReferenceFrame.SWITZERLAND;
		File file = new File("/media/datahaki/media/ethz/taxi", "2017-06-27 - GPS Fahrtstrecken-Protokoll.csv");
		DayTaxiRecord dayTaxiRecord = new DayTaxiRecord();
		// extract from csv file
		CsvFleetReader reader = new CsvFleetReader(dayTaxiRecord);
		reader.populate(file);
		Network network = NetworkLoader.loadNetwork(args);
		MatsimStaticDatabase.initializeSingletonInstance(network, referenceFrame);
		// generate sim objects and store
		SimulationFleetDump.of(dayTaxiRecord, network, MatsimStaticDatabase.INSTANCE);
	}

}
