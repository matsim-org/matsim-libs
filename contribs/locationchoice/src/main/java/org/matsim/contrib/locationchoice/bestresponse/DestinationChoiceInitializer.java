package org.matsim.contrib.locationchoice.bestresponse;

import org.apache.log4j.Logger;
import org.matsim.contrib.locationchoice.analysis.DistanceStats;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.MaxDCScoreWrapper;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrComputeMaxDCScore;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.controler.Controler;
import org.matsim.utils.objectattributes.ObjectAttributes;


/*
 * Listener for inclusion of bestreply lc, very similar to roadpricing
 * no further coding should be required 
 */
public class DestinationChoiceInitializer implements StartupListener {
	private DestinationChoiceBestResponseContext lcContext;
	private ObjectAttributes personsMaxDCScoreUnscaled;
	private static final Logger log = Logger.getLogger(DestinationChoiceInitializer.class);
	

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();  		
  		lcContext = new DestinationChoiceBestResponseContext(event.getControler().getScenario()); 		
  		  		
  		// compute or read maxDCScore but do not add it to the context:
  		// context can then be given to scoring classes both during regular scoring and in pre-processing 
  		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(lcContext);
  		computer.readOrCreateMaxDCScore(controler, lcContext.kValsAreRead());
  		this.personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();
  		 		
		/* 
		 * add ScoringFunctionFactory to controler
		 *  in this way scoringFunction does not need to create new, identical k-vals by itself    
		 */
  		DCScoringFunctionFactory dcScoringFunctionFactory = new DCScoringFunctionFactory(controler.getConfig(), controler, lcContext); 	
		controler.setScoringFunctionFactory(dcScoringFunctionFactory);
		// dcScoringFunctionFactory.setUsingFacilityOpeningTimes(false); // TODO: make this configurable
		
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "s", lcContext.getConverter()));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "l", lcContext.getConverter()));
				
		MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
		dcScore.setPersonsMaxDCScoreUnscaled(personsMaxDCScoreUnscaled);
		controler.getScenario().addScenarioElement(lcContext);
		controler.getScenario().addScenarioElement(dcScore);
		
		// TODO: check why this is required here now. order of calling probably.
		controler.getScenario().addScenarioElement(new FacilityPenalties()); 
		log.info("lc initialized");
	}	
}
