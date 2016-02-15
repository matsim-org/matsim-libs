package playground.balac.avignon;

import org.apache.log4j.Logger;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;



public class AvignonControler {
	private static final Logger log = Logger.getLogger( AvignonControler.class ) ;
	
	private DestinationChoiceBestResponseContext dcContext;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new AvignonControler().run(args);
	}

	private void run(String[] args) {
		Controler controler = new Controler(args);
		//controler.setOverwriteFiles(true);
				/*
		 * would be muuuuch nicer to have this in DestinationChoiceInitializer, but startupListeners are called after corelisteners are called
		 * -> scoringFunctionFactory cannot be replaced
		 */
		dcContext = new DestinationChoiceBestResponseContext(controler.getScenario());
		/*
		 * add ScoringFunctionFactory to controler
		 *  in this way scoringFunction does not need to create new, identical k-vals by itself
		 */
		AvignonScoringFunctionFactory dcScoringFunctionFactory = new AvignonScoringFunctionFactory(controler.getConfig(), controler, this.dcContext);
		controler.setScoringFunctionFactory(dcScoringFunctionFactory);


		if (!controler.getConfig().findParam("locationchoice", "prefsFile").equals("null") &&
				!controler.getConfig().facilities().getInputFile().equals("null")) {
			dcScoringFunctionFactory.setUsingConfigParamsForScoring(false);
		} else {
			dcScoringFunctionFactory.setUsingConfigParamsForScoring(true);
			log.info("external prefs are not used for scoring!");
		}

		dcContext.init(); // this is an ugly hack, but I somehow need to get the scoring function + context into the controler

		controler.addControlerListener(new DestinationChoiceInitializer(this.dcContext));

		if (Double.parseDouble(controler.getConfig().findParam("locationchoice", "restraintFcnExp")) > 0.0 &&
				Double.parseDouble(controler.getConfig().findParam("locationchoice", "restraintFcnFactor")) > 0.0) {
			controler.addControlerListener(new FacilitiesLoadCalculator(this.dcContext.getFacilityPenalties()));
		}
		//this.addControlerListener(new RetailersLocationListener());
		//this.addControlerListener(new DistanceControlerListener(1.0));

//		super.loadControlerListeners();

		controler.run();

	}

}
