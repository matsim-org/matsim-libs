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
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.BestPlanSelector;
import org.matsim.replanning.selectors.RandomPlanSelector;


/**
 * Test of TimeWriter and BottleneckTravol
 *
 * @author ychen
 */
public class BottleneckControler extends Controler {

	private final TimeWriter timeWriter;
	private final BottleneckTraVol bTV;

	public BottleneckControler() {
		super();
		this.timeWriter = new TimeWriter(getOutputFilename("bottleneckTime.txt"));
		this.bTV = new BottleneckTraVol(getOutputFilename("bottleneckTraVol.txt"));
	}

	@Override
	protected void setupIteration(final int iteration) {
		super.setupIteration(iteration);
		if (iteration == 1000) {
			this.events.addHandler(this.timeWriter);
			this.events.addHandler(this.bTV);
		}
		if (iteration == 0) {
			this.config.simulation().setSnapshotPeriod(0);
		}
		if (iteration == 1000) {
			this.config.simulation().setSnapshotPeriod(60);
		}
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		PlanStrategy strategy1 = new PlanStrategy(new RandomPlanSelector());
		strategy1.addStrategyModule(new TimeAllocationMutatorBottleneck());
		manager.addStrategy(strategy1, 0.10);
		PlanStrategy strategy2 = new PlanStrategy(new BestPlanSelector());
		manager.addStrategy(strategy2, 0.90);

		return manager;
	}

	public static void main(final String[] args) {
		final BottleneckControler ctl = new BottleneckControler();
		System.out.println(args);
		ctl.run(args);
		System.out.println(args);
		ctl.timeWriter.closefile();
		ctl.bTV.closefile();
		System.exit(0);
	}
}
