/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayControler.java
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

package playground.gregor.withindayevac.controler;



import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.controler.corelisteners.PlansDumping;
import org.matsim.controler.corelisteners.PlansReplanning;
import org.matsim.controler.corelisteners.PlansScoring;
import org.matsim.evacuation.EvacuationQSimControler;
import org.matsim.mobsim.queuesim.QueueSimulation;

import playground.gregor.withindayevac.PlanGenerator;
import playground.gregor.withindayevac.mobsim.WithindayQueueSimulation;

public class WithindayControler extends EvacuationQSimControler {

	private final static Logger log = Logger.getLogger(WithindayControler.class);
	
	public WithindayControler(final String[] args) {
		super(args);
	}

	@Override
	protected void setup() {
		super.setup();
		log.info("adding additional guide agents...");
//		new RoadSignAgentsGenerator().generateGuides(this.population,this.network);
		log.info("done.");
	}

	@Override
	protected void runMobSim() {
//		log.info("Starting withinday replanning iteration...");

		if (getIteration() < 300 && getIteration() > 0) {
			//build the queuesim
			final WithindayQueueSimulation sim = new WithindayQueueSimulation(this.network, this.population, this.events, this);
			//run the simulation
			sim.run();
		} else {
			QueueSimulation sim = new QueueSimulation(this.network,this.population, this.events);
			sim.run();
		}
	
	}

	@Override
	protected void loadCoreListeners() {
		/* The order how the listeners are added is very important!
		 * As dependencies between different listeners exist or listeners
		 * may read and write to common variables, the order is important.
		 * Example: The RoadPricing-Listener modifies the scoringFunctionFactory,
		 * which in turn is used by the PlansScoring-Listener.
		 * Note that the execution order is contrary to the order the listeners are added to the list.
		 */

		this.addCoreControlerListener(new CoreControlerListener());

		this.addCoreControlerListener(new PlanGenerator());
		
		// the default handling of plans
//		this.addCoreControlerListener(new AggregatedPlansScoring());
//		this.addCoreControlerListener(new SelectedPlansScoring());
		this.addCoreControlerListener(new PlansScoring());


		this.addCoreControlerListener(new PlansReplanning());
		this.addCoreControlerListener(new PlansDumping());
	}

	public static void main(final String[] args) {
		final Controler controler = new WithindayControler(args);
		controler.run();
		System.exit(0);
	}
	
}
