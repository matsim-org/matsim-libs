/* *********************************************************************** *
 * project: org.matsim.*
 * TimeControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.yu.bottleneck;

import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.replanning.selectors.RandomPlanSelector;

/**
 * @author ychen
 */
public class BottleneckControler extends Controler {

	public BottleneckControler(final String[] args) {
		super(args);
	}

	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		manager.setMaxPlansPerAgent(5);
		//
		PlanStrategy strategy1 = new PlanStrategy(new ExpBetaPlanSelector());
		manager.addStrategy(strategy1, 0.95);

		PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
		strategy2.addStrategyModule(new TimeAllocationMutatorBottleneck());
		manager.addStrategy(strategy2, 0.05);
		return manager;
	}

	public static class BottleneckControlerListener implements ShutdownListener, IterationStartsListener {
		private final TimeWriter timeWriter = new TimeWriter("./test/yu/Bottleneck/outputbottleneckTime.txt");
		private final BottleneckTraVol bTV = new BottleneckTraVol("./test/yu/Bottleneck/outputbottleneckTraVol.txt");

		public void notifyIterationStarts(final IterationStartsEvent event) {
			if (event.getIteration() == 0) {
				event.getControler().getConfig().simulation().setSnapshotPeriod(0);
			}
			if (event.getIteration() == 1000) {
				event.getControler().getEvents().addHandler(this.timeWriter);
				event.getControler().getEvents().addHandler(this.bTV);
				event.getControler().getConfig().simulation().setSnapshotPeriod(60);
			}
		}

		public void notifyShutdown(final ShutdownEvent event) {
			this.timeWriter.closefile();
			this.bTV.closefile();
		}
	}

	// -------------------------MAIN FUNCTION-------------------
	public static void main(final String[] args) {
		final BottleneckControler ctl = new BottleneckControler(args);
		ctl.addControlerListener(new BottleneckControlerListener());
		ctl.run();
		System.exit(0);
	}
}
