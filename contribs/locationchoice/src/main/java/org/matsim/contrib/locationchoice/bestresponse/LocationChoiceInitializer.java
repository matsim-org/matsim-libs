package org.matsim.contrib.locationchoice.bestresponse;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.BestReplyLocationChoicePlanStrategy;
import org.matsim.contrib.locationchoice.analysis.DistanceStats;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ReadOrComputeMaxDCScore;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DCScoringFunctionFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;


/*
 * Listener for inclusion of bestreply lc, very similar to roadpricing
 * no further coding should be required 
 */
public class LocationChoiceInitializer implements StartupListener {
	private LocationChoiceBestResponseContext lcContext;
	private ObjectAttributes personsMaxDCScoreUnscaled;
	private static final Logger log = Logger.getLogger(LocationChoiceInitializer.class);
	

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();  		
  		lcContext = new LocationChoiceBestResponseContext(event.getControler().getScenario()); 		
  		  		
  		// compute or read maxDCScore but do not add it to the context:
  		// context can then be given to scoring classes both during regular scoring and in pre-processing 
  		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(lcContext);
  		computer.readOrCreateMaxDCScore(controler, lcContext.kValsAreRead());
  		this.personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();
  		 		
		/* 
		 * add ScoringFunctionFactory to controler
		 *  in this way scoringFunction does not need to create new, identical k-vals by itself    
		 */
  		DCScoringFunctionFactory mixedScoringFunctionFactory = new DCScoringFunctionFactory(controler.getConfig(), controler, lcContext); 	
		controler.setScoringFunctionFactory(mixedScoringFunctionFactory);
		
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "s", lcContext.getConverter()));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "l", lcContext.getConverter()));
		
		controler.addPlanStrategyFactory("BestReplyLocationChoice", new PlanStrategyFactory(){
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario2, EventsManager eventsManager) {
				return new BestReplyLocationChoicePlanStrategy(lcContext, personsMaxDCScoreUnscaled);
			}
		});
		log.info("lc initialized");
	}	
}
