/* *********************************************************************** *
 * project: org.matsim.*
 * PsimAwareEventsWriter.java
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
package playground.thibautd.pseudoqsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * @author thibautd
 */
public class PsimAwareEventsWriter implements BasicEventHandler, ScoringListener {
	private static final Logger log =
		Logger.getLogger(PsimAwareEventsWriter.class);

	private final PseudoSimConfigGroup config;
	private final OutputDirectoryHierarchy controlerIO;
	private EventWriterXML delegate = null;

	private final int lastIteration;

	public PsimAwareEventsWriter(
			final OutputDirectoryHierarchy controlerIO,
			final int lastIteration,
			final PseudoSimConfigGroup config ) {
		this.controlerIO = controlerIO;
		this.config = config;
		this.lastIteration = lastIteration;
	}

	@Override
	public void reset(final int iter) {
		if ( closeAndNullifyDelegate() ) {
			log.warn( "events file was not closed before reset. This might cause problems in the last iteration." );
			log.warn( "was "+getClass().getSimpleName()+" added as a Controler Listenner?" );
		}

		if ( iter == lastIteration || config.isDumpingIter( iter ) ) {
			this.delegate =
				new EventWriterXML(
						controlerIO.getIterationFilename(
							iter,
							"events.xml.gz" ) );
			// do not call reset on delegate: it does close the file,
			// which would then have to be manually re-opened by calling
			// delegate.init( fileName )...
			// delegate.reset( iter );
		}
	}

	@Override
	public void handleEvent(final Event event) {
		if ( delegate == null ) return;
		delegate.handleEvent( event );
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		closeAndNullifyDelegate();
	}

	private final boolean closeAndNullifyDelegate() {
		if ( delegate == null ) return false;

		delegate.closeFile();
		delegate = null;
		return true;
	}
}

