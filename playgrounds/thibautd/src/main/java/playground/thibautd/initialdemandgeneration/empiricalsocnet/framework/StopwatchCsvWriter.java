/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.config.groups.ControlerConfigGroup;

import java.util.Collections;
import java.util.Set;

/**
 * @author thibautd
 */
@Singleton
public class StopwatchCsvWriter extends AbstractCsvWriter {
	private final long startTime;
	private int cliqueNr = 0;

	@Inject
	protected StopwatchCsvWriter(
			final ControlerConfigGroup config,
			final SocialNetworkSampler sampler,
			final AutocloserModule.Closer closer ) {
		super( config.getOutputDirectory() +"/cliquesStopWatch.dat", sampler, closer );
		this.startTime = System.currentTimeMillis();
	}

	@Override
	protected String titleLine() {
		return "cliqueNr\ttotalElapsedTime_ms";
	}

	@Override
	protected Iterable<String> cliqueLines( final Set<Ego> clique ) {
		return Collections.singleton( (cliqueNr++ )+"\t"+(System.currentTimeMillis() - startTime) );
	}
}
