/* *********************************************************************** *
 * project: org.matsim.*
 * JointTimeModeChooserModule.java
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
package playground.thibautd.cliquessim.replanning.modules.jointtimemodechooser;

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author thibautd
 */
public class JointTimeModeChooserModule extends AbstractMultithreadedModule {
	private static final boolean DUMP_STATS = true;
	private final Controler controler;
	private final DepartureDelayAverageCalculator delay;
	private StatisticsCollector statsCollector = null;
	private int currentIteration = -1;

	public JointTimeModeChooserModule(final Controler controler) {
		super( controler.getConfig().global() );
		this.controler = controler;
		delay = new DepartureDelayAverageCalculator(
				controler.getNetwork(),
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new JointTimeModeChooserAlgorithm(
				MatsimRandom.getLocalInstance(),
				statsCollector,
				delay,
				controler);
	}

//	@Override
//	public void prepareReplanning(ReplanningContext replanningContext) {
//		statsCollector = DUMP_STATS ?
//			new StatisticsCollector() :
//			null;
//		super.prepareReplanning(replanningContext);
//	}
	
	@Override
	public void beforePrepareReplanningHook( ReplanningContext replanningContext ) {
		this.currentIteration = replanningContext.getIteration();
		statsCollector = DUMP_STATS ?
		new StatisticsCollector() :
		null;
	}

//	@Override
//	public void finishReplanning() {
//		super.finishReplanning();
//		if (statsCollector != null) {
//			statsCollector.dumpStatistics(
//					controler.getControlerIO().getIterationFilename(
//						controler.getIterationNumber(),
//						getClass().getSimpleName()+"Stats.dat" ));
//		}
//	}
	
	@Override
	protected void afterFinishReplanningHook() {
		if (statsCollector != null) {
			statsCollector.dumpStatistics(
					controler.getControlerIO().getIterationFilename(
							this.currentIteration,
							getClass().getSimpleName()+"Stats.dat" ));
		}
	}
	
}
