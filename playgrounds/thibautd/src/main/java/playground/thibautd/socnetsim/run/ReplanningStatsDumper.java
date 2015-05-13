/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningStatsDumper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.run;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.socnetsim.framework.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.framework.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public class ReplanningStatsDumper implements StartupListener, BeforeMobsimListener, GroupStrategyManager.Listener, ShutdownListener {
	private final String outPath;

	private BufferedWriter writer;

	public ReplanningStatsDumper( final String outFile ) {
		this.outPath = outFile;
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		this.writer = IOUtils.getBufferedWriter( outPath );
		try {
			this.writer.write( "iter\tgroupSize\tstrategy" );
		} 
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}


	@Override
	public void notifyAlloc(
			final int iteration,
			final ReplanningGroup g,
			final GroupPlanStrategy strategy) {
		try {
			this.writer.newLine();
			this.writer.write( ""+iteration );
			this.writer.write( "\t" );
			this.writer.write( ""+g.getPersons().size() );
			this.writer.write( "\t" );
			this.writer.write( strategy.toString() );
		} 
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			this.writer.close();
		} 
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		try {
			this.writer.flush();
		} 
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}
}

