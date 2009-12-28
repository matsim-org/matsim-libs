/* *********************************************************************** *
 * project: org.matsim.*
 * FirstPersonPlanScoreMonitor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.replanning;

import java.util.Random;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ControlerEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class OnePersonPlanScoreMonitor implements BeforeMobsimListener,
		IterationEndsListener, StartupListener, ShutdownListener {
	private SimpleWriter writer = null;

	private void writeFirstPlans(ControlerEvent event) {
		for (Plan plan : event.getControler().getPopulation().getPersons()
				.values().iterator().next().getPlans()) {
			writer.write("\t" + plan.getScore().toString());
		}
		writer.writeln();
		writer.flush();
	}

	private void writeOnePersonPlans(ControlerEvent event, int id) {
		for (Plan plan : event.getControler().getPopulation().getPersons().get(
				new IdImpl(id)).getPlans()) {
			writer.write("\t" + plan.getScore().toString());
		}
		writer.writeln();
		writer.flush();
	}

	private void writeRandom10SelectedPlans(ControlerEvent event) {
		Random r = MatsimRandom.getLocalInstance();
		r.setSeed(4711);
		for (int i = 0; i < 10; i++) {
			int id = r.nextInt(100);
			Plan plan = event.getControler().getPopulation().getPersons().get(
					new IdImpl(id)).getSelectedPlan();
			writer.write("\t" + id);
			writer.write("\t" + plan.getScore().toString());
		}
		writer.writeln();
		writer.flush();
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// writer.write("ITERATION " + event.getIteration());
		// writePlans(event);
		// writeOnePersonPlans(event, 62);
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		writer.write("ITERATION " + event.getIteration());
		// writeFirstPlans(event);
		// writeRandom10SelectedPlans(event);
		writeOnePersonPlans(event, 62);
	}

	public void notifyStartup(StartupEvent event) {
		event.getControler();
		writer = new SimpleWriter(Controler
				.getOutputFilename("onePlanScores_ceb6_rop.txt"));
	}

	public void notifyShutdown(ShutdownEvent event) {
		writer.close();
	}

}
