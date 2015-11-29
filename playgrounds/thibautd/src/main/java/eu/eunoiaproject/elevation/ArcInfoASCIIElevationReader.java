/* *********************************************************************** *
 * project: org.matsim.*
 * ArcInfoASCIIElevationReader.java
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
package eu.eunoiaproject.elevation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Reads elevation data in the ArcInfo ASCII exchange format.
 * see http://docs.codehaus.org/display/GEOTOOLS/ArcInfo+ASCII+Grid+format
 *
 * <br>
 * This is the format of SRTM data.
 * see http://srtm.csi.cgiar.org/SRTMdataProcessingMethodology.asp
 * Data from NASA satellite radars.
 * Much easier to import than data of swisstopo, open data and available
 * world-wide.
 *
 * @author thibautd
 */
public class ArcInfoASCIIElevationReader {
	private static final Logger log =
		Logger.getLogger(ArcInfoASCIIElevationReader.class);

	public static GridElevationProvider read(
			final String file) {
		final BufferedReader reader = IOUtils.getBufferedReader( file );

		try {
			log.info( "reading metadata from "+file );
			final GridElevationProvider elevation = parseMetadata( reader );
			log.info( "reading actual data from "+file );
			parseData( reader , elevation );
			return elevation;
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
		finally {
			try {
				reader.close();
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}
	}

	private static enum Metadata {
		ncols, 
		nrows,
		xllcorner,
		yllcorner,
		cellsize,
		NODATA_value;
	}
	private static GridElevationProvider parseMetadata( final BufferedReader reader ) throws IOException {
		int nCols = -1;
		int nRows = -1;
		double llx  = Double.NaN;
		double lly  = Double.NaN;
		double cellSize  = Double.NaN;

		while ( nCols < 0 || nRows < 0 || Double.isNaN( llx ) || Double.isNaN( llx ) || Double.isNaN( cellSize ) ) {
			final String line = reader.readLine();
			final String[] split = line.split( " " );

			switch ( Metadata.valueOf( split[ 0 ] ) ) {
			case cellsize:
				cellSize = Double.valueOf( split[ split.length - 1 ] );
				break;
			case ncols:
				nCols = Integer.valueOf( split[ split.length - 1 ] );
				break;
			case nrows:
				nRows = Integer.valueOf( split[ split.length - 1 ] );
				break;
			case xllcorner:
				llx = Double.valueOf( split[ split.length - 1 ] );
				break;
			case yllcorner:
				lly = Double.valueOf( split[ split.length - 1 ] );
				break;
			case NODATA_value:
				break;
			default:
				throw new RuntimeException();
			}
		}

		return new GridElevationProvider(
				nCols,
				nRows,
				new Coord(llx, lly),
				cellSize );
	}

	private static void parseData( final BufferedReader reader , final GridElevationProvider elevation ) throws IOException {
		final Counter counter = new Counter( "reading row # " );
		int lineNr = 0;
		for ( String line = reader.readLine();
				line != null;
				line = reader.readLine() ) {
			if ( line.startsWith( Metadata.NODATA_value.toString() ) ) continue;
			counter.incCounter();
			final String[] values = line.split( " " );
			for ( int i=0; i < values.length; i++ ) {
				elevation.putAltitude(
						i,
						// data goes from upper left to lower right...
						elevation.getNRows() - lineNr - 1,
						Double.valueOf( values[ i ] ) );
			}
			lineNr++;
		}
		counter.printCounter();
	}
}

