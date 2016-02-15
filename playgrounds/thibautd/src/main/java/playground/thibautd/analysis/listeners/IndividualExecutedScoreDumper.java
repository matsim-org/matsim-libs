/* *********************************************************************** *
 * project: org.matsim.*
 * IndividualExecutedScoreDumper.java
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
package playground.thibautd.analysis.listeners;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author thibautd
 */
public class IndividualExecutedScoreDumper implements IterationEndsListener, ShutdownListener {
	private static final Logger log =
		Logger.getLogger(IndividualExecutedScoreDumper.class);


	final private Collection<Person> persons;
	final private String filename;

	private BufferedWriter out;
	
	public IndividualExecutedScoreDumper(
			final Collection<? extends Person> persons,
			final String filename){
		this.persons = new ArrayList<Person>( persons );
		this.filename = filename;
	}

	private void createOut() {
		this.out = IOUtils.getBufferedWriter(filename);

		try {
			this.out.write("ITERATION");

			for ( Person p : persons ) {
				this.out.write( "\tscore_"+p.getId() );
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		// need to wait for controler to create the directories.
		if ( this.out == null ) createOut();
		try {
			this.out.newLine();
			this.out.write( ""+event.getIteration() );

			for ( Person person : this.persons ) {
				this.out.write( "\t"+person.getSelectedPlan().getScore() );
			}
		} catch (IOException e) {
			// this is not fatal: continue without crash
			log.error( "error while writing to file. Not fatal: continue..." , e );
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			// this is not fatal: continue without crash
			log.error( "error while writing to file. Not fatal: continue..." , e );
		}
	}
}
