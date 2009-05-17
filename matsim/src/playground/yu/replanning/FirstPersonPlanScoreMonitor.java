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

import org.matsim.core.api.population.Plan;
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

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class FirstPersonPlanScoreMonitor implements BeforeMobsimListener,
		IterationEndsListener, StartupListener, ShutdownListener {
	private SimpleWriter writer = null;

	private void writePlans(ControlerEvent event) {
		for (Plan plan : event.getControler().getPopulation().getPersons()
				.values().iterator().next().getPlans()) {
			writer.write("\t" + plan.getScore().toString());
			// if (plan.isSelected())
			// writer.write('s');
		}
		writer.writeln();
		writer.flush();
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		// writer.write("ITERATION " + event.getIteration());
		// writePlans(event);
	}

	public void notifyIterationEnds(IterationEndsEvent event) {
		writer.write("ITERATION " + event.getIteration());
		writePlans(event);
	}

	public void notifyStartup(StartupEvent event) {
		event.getControler();
		writer = new SimpleWriter(Controler
				.getOutputFilename("firstPersonPlanScores.txt"));
	}

	public void notifyShutdown(ShutdownEvent event) {
		writer.close();
	}

}
