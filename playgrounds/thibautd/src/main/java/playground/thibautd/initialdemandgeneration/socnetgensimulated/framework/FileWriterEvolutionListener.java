/* *********************************************************************** *
 * project: org.matsim.*
 * FileWriterEvolutionListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author thibautd
 */
public class FileWriterEvolutionListener implements ModelIterator.EvolutionListener {
	private static final Logger log =
		Logger.getLogger(FileWriterEvolutionListener.class);

	private final BufferedWriter writer;

	// do not crash on IO exception. Its a shame not to get a nice file
	// to analyse evolution, but it would be even more stupid to abort valid calculations
	// because of this.
	private boolean doAnalyse = true;

	public FileWriterEvolutionListener( final String fileName ) {
		this.writer = IOUtils.getBufferedWriter( fileName );
		
		try {
			this.writer.write( "primaryThreshold\tsecondaryReduction\tavgDegree\tclustering" );
		}
		catch ( IOException e ) {
			log.error( "problem while opening file "+fileName , e );
			log.error( this+" will not write anything to file!" );
			doAnalyse = false;
		}
	}

	@Override
	public void handleMove( final Thresholds m ) {
		if ( !doAnalyse ) return;
		try {
			writer.newLine();
			writer.write(
					m.getPrimaryThreshold()+"\t"+
					m.getSecondaryReduction()+"\t"+
					m.getResultingAverageDegree()+"\t"+
					m.getResultingClustering() );
			// make sure results are immediately available
			writer.flush();
		}
		catch ( IOException e ) {
			log.error( "problem while writing to file" , e );
			log.error( this+" now stops writing information!" );
			doAnalyse = false;
		}
	}

	public void close() {
		try {
			writer.close();
		}
		catch ( IOException e ) {
			log.error( "problem while closing file" , e );
		}
	}
}

