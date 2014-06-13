/* *********************************************************************** *
 * project: org.matsim.*
 * DgController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.dgrether;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class DgController {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler c = new Controler(args[0]);
		c.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		DgSylviaConfig sylviaConfig = new DgSylviaConfig();
		final DgSylviaControlerListenerFactory signalsFactory = new DgSylviaControlerListenerFactory(sylviaConfig);
		signalsFactory.setAlwaysSameMobsimSeed(false);
		c.setSignalsControllerListenerFactory(signalsFactory);
		c.setOverwriteFiles(true);
		
		if ( false ) {
			IterationStartsListener strategyWeightsManager = new IterationStartsListener() {
				@Override
				public void notifyIterationStarts(IterationStartsEvent event) {

					GenericPlanStrategy<Plan> strategy 
					= new PlanStrategyImpl(new ExpBetaPlanChanger(Double.NaN) );
					// (dummy strategy, just to get the type.  Not so great.  Not even sure if it will work. Ask MZ. Kai)

					String subpopulation= null ;
					// (I think this is just null. Kai)

					double newWeight = 1./event.getIteration() ;
					// (program function as you want/need)

					event.getControler().getStrategyManager().changeWeightOfStrategy(strategy, subpopulation, newWeight) ;
				}
			} ;
			c.addControlerListener(strategyWeightsManager);
		}
		
		c.run();
	}

}
