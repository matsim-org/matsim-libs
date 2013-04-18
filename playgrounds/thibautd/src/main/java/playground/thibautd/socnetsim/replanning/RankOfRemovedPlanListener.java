/* *********************************************************************** *
 * project: org.matsim.*
 * RankOfRemovedPlanListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author thibautd
 */
public class RankOfRemovedPlanListener implements GroupStrategyManager.RemovedPlanListener, IterationStartsListener, ShutdownListener , BeforeMobsimListener {
	private final BufferedWriter writer;
	private int iteration = -1;

	public RankOfRemovedPlanListener(final String fileName) {
		this.writer = IOUtils.getBufferedWriter( fileName );
		try {
			writer.write( "iteration\trankRemovedPlan\tnPlans" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		try {
			writer.close();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		iteration = event.getIteration();
	}

	@Override
	public void notifyPlanRemovedForAgent(
			final Plan plan,
			final Person person) {
		int nPlansWithBetterScore = 0;
		int nTimesPlanFound = 0;

		for ( Plan iterated : person.getPlans() ) {
			if ( iterated == plan ) nTimesPlanFound++;
			if ( iterated.getScore() >= plan.getScore() ) nPlansWithBetterScore++;
		}

		if ( nTimesPlanFound != 1 ) throw new RuntimeException( nTimesPlanFound+"" );

		try {
			writer.newLine();
			writer.write( iteration +"\t"+ nPlansWithBetterScore + "\t" + person.getPlans().size() );
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		try {
			writer.flush();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}
}

