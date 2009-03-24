package playground.anhorni.locationchoice.preprocess.analyzePlansAndFacs;

import org.apache.log4j.Logger;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

public class AnalyzePlansAndFacs {

	private final static Logger log = Logger.getLogger(AnalyzePlansAndFacs.class);
	private Facilities facilitiesAll;
	private Facilities facilitiesSL;
	private NetworkLayer network;
	
	public static void main(final String[] args) {

		Gbl.startMeasurement();
		final AnalyzePlansAndFacs analyzer = new AnalyzePlansAndFacs();
		if (args.length < 1) {
			log.info("Too few arguments!");
			System.exit(0);
		}
		analyzer.run("input/networks/ivtch.xml", args[0], "input/facilities.xml.gz", "input/facilities_KTIYear2.xml.gz");
		Gbl.printElapsedTime();
	}
	
	public void run(String networkfilePath, String plansfilePath, String facilitiesAllfilePath, String facilitiesSLfilePath) {
		
		log.info("reading the facilities ...");
		this.facilitiesAll =(Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilitiesAll).readFile(facilitiesAllfilePath);
			
		log.info("reading the network ...");
		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		
		log.info("analyze plans ...");
		AnalyzePlans plansAnalyzer = new AnalyzePlans();
		plansAnalyzer.run(plansfilePath, this.facilitiesAll, this.network);
		
		Gbl.getWorld().getLayers().remove(Facilities.LAYER_TYPE);
		this.facilitiesSL = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilitiesSL).readFile(facilitiesSLfilePath);
		
		log.info("analyze facilities ...");
		AnalyzeFacilities facilitiesAnalyzer = new AnalyzeFacilities();
		facilitiesAnalyzer.run(this.facilitiesSL, network);				
	}
}
