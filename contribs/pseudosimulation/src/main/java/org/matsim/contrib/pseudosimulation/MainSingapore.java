package org.matsim.contrib.pseudosimulation;

import org.apache.log4j.Logger;
import org.matsim.contrib.pseudosimulation.controler.PSimControler;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;


public class MainSingapore {

	/**
	 * @param args - The name of the config file for the psim run.
	 */
	public static void main(String[] args) {
		Logger.getLogger("PSim").warn("Running a PSEUDOSIMULATION");
		PSimControler c = new PSimControler(args);
        c.getMATSimControler().getConfig().controler().setCreateGraphs(false);
        c.setTransitRouterFactory(new TransitRouterEventsWSFactory(c.getScenario(), c.getWaitTimeCalculator().getWaitTimes(), c.getStopStopTimeCalculator().getStopStopTimes()));
		c.setScoringFunctionFactory(new CharyparNagelOpenTimesScoringFunctionFactory(c.getScenario().getConfig().planCalcScore(), c.getScenario()));
		c.getMATSimControler().run();
		
	}

}
