/* *********************************************************************** *
 * project: org.matsim.*
 * ModelIteratorFileListener.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author thibautd
 */
public class ModelIteratorFileListener implements ModelIterator.IterationListener {
	private static final Logger log =
		Logger.getLogger(ModelIteratorFileListener.class);

	private final BufferedWriter writer;

	public ModelIteratorFileListener(final String file) {
		writer = IOUtils.getBufferedWriter( file );

		try {
			writer.write( "primaryThreshold\tsecondaryThreshold\tavgPersNetSize\tclusteringCoef" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void notifyStats(
			final ThresholdFunction thresholds,
			final double avgPersonalNetworkSize,
			final double clusteringCoefficient) {
		try {
			writer.newLine();
			writer.write( thresholds.getPrimaryTieThreshold()+"\t"+
					thresholds.getSecondaryTieThreshold()+"\t"+
					avgPersonalNetworkSize+"\t"+
					clusteringCoefficient );
			// make the line visible immediately
			writer.flush();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public void close() {
		try {
			writer.close();
		}
		catch (IOException e) {
			// not so bad: do not fail
			log.error( "error while closing file" , e );
		}
	}
}

