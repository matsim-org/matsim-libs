package org.matsim.contrib.locationchoice.bestresponse;

import org.matsim.contrib.locationchoice.analysis.DistanceStats;
import org.matsim.contrib.locationchoice.bestresponse.scoring.MixedScoringFunctionFactory;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.controler.Controler;


/*
 * Listener for inclusion of bestreply lc, very similar to roadpricing
 * no further coding should be required 
 */
public class LocationChoiceInitializer implements StartupListener {

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();  		
  		LocationChoiceBestResponseContext lcContext = new LocationChoiceBestResponseContext(event.getControler().getScenario());
  		lcContext.init(); // For me easier to read with init.
  		
  		  		
  		// compute or read maxDCScore but do not add it to the context:
  		// context can then be given to scoring classes both during regular scoring and in pre-processing 

		
  		
		/* 
		 * add ScoringFunctionFactory to controler
		 *  in this way scoringFunction does not need to create new, identical k-vals by itself    
		 */
  		MixedScoringFunctionFactory mixedScoringFunctionFactory =
			new MixedScoringFunctionFactory(controler.getConfig(), controler, 
					lcContext.getScaleEpsilon(), lcContext.getConverter(), lcContext.getFlexibleTypes()); 	
		controler.setScoringFunctionFactory(mixedScoringFunctionFactory);
		
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "s", lcContext.getConverter()));
		controler.addControlerListener(new DistanceStats(controler.getConfig(), "best", "l", lcContext.getConverter()));		
	}
}
