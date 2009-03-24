package playground.anhorni.locationchoice.preprocess.analyzePlansAndFacs;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;

public class AnalyzePlansAndFacs {

	private final static Logger log = Logger.getLogger(AnalyzePlansAndFacs.class);
	
	public static void main(final String[] args) {

		Gbl.startMeasurement();
		final AnalyzePlansAndFacs analyzer = new AnalyzePlansAndFacs();
		if (args.length < 2) {
			log.info("Too few arguments!");
			System.exit(0);
		}
		analyzer.run(args[0], args[1]);
		Gbl.printElapsedTime();
	}
	
	public void run(String plansfilePath, String networkfilePath) {
		AnalyzePlans plansAnalyzer = new AnalyzePlans();
		plansAnalyzer.run(networkfilePath, plansfilePath);
	}
	
}
