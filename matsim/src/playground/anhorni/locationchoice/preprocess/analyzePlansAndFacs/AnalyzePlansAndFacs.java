package playground.anhorni.locationchoice.preprocess.analyzePlansAndFacs;

import org.apache.log4j.Logger;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

public class AnalyzePlansAndFacs {

	private final static Logger log = Logger.getLogger(AnalyzePlansAndFacs.class);
	private Facilities facilities;
	private NetworkLayer network;
	
	public static void main(final String[] args) {

		Gbl.startMeasurement();
		final AnalyzePlansAndFacs analyzer = new AnalyzePlansAndFacs();
		if (args.length < 1) {
			log.info("Too few arguments!");
			System.exit(0);
		}
		analyzer.run("input/networks/ivtch.xml", args[0], "input/facilities.xml.gz");
		Gbl.printElapsedTime();
	}
	
	public void run(String networkfilePath, String plansfilePath, String facilitiesfilePath) {
		
		log.info("reading the facilities...");
		this.facilities=(Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		
		log.info("reading the network...");
		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		
		
		AnalyzePlans plansAnalyzer = new AnalyzePlans();
		plansAnalyzer.run(plansfilePath, this.facilities, this.network);
	}
}
