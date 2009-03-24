package playground.anhorni.locationchoice.preprocess.analyzePlansAndFacs;

import org.apache.log4j.Logger;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;

public class AnalyzePlansAndFacs {

	private final static Logger log = Logger.getLogger(AnalyzePlansAndFacs.class);
	private Facilities facilities;
	
	public static void main(final String[] args) {

		Gbl.startMeasurement();
		final AnalyzePlansAndFacs analyzer = new AnalyzePlansAndFacs();
		if (args.length < 3) {
			log.info("Too few arguments!");
			System.exit(0);
		}
		analyzer.run(args[0], args[1], args[2]);
		Gbl.printElapsedTime();
	}
	
	public void run(String networkfilePath, String plansfilePath, String facilitiesfilePath) {
		
		this.facilities=(Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		log.info("facilities reading done");
		
		
		AnalyzePlans plansAnalyzer = new AnalyzePlans();
		plansAnalyzer.run(networkfilePath, plansfilePath, this.facilities);
	}
	
}
