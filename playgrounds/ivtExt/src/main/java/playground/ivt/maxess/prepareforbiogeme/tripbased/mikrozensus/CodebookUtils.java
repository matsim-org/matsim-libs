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
package playground.ivt.maxess.prepareforbiogeme.tripbased.mikrozensus;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.ivt.utils.MoreIOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author thibautd
 */
public class CodebookUtils {
	public static void writeCodebook(
			final String filename,
			final Codebook codebook ) {
		try ( final BufferedWriter writer = IOUtils.getBufferedWriter( filename ) ) {
			MoreIOUtils.writeLines(
					writer,
					"Information",
					"===========",
					"This is metadata to dataset generated with:",
					"",
					"`"+getMainClassName()+"`",
					"",
					"Date: " + DateFormat.getDateInstance(
							DateFormat.FULL ).format(
									new Date() ),
					"",
					"",
					"Conversion to pdf: use [pandoc](http://pandoc.org/README.html)",
					"",
					"Command: `pandoc Codebook.md -o Codebook.pdf`",
					"",
					"Codebook",
					"========"
					);

			for ( Codebook.Codepage page : codebook.getPages().values() ) {
				writer.write( page.getVariableName() );
				writer.newLine();

				for ( int i=0; i < page.getVariableName().length(); i++ ) writer.write( "-" );
				writer.newLine();
				writer.newLine();

				// Probably not supernice, one should adapt column width to some extent.
				// on the other side, one can simply use pandoc to get a PDF version
				writer.write( "| Code | Meaning | Count |" );
				writer.newLine();
				writer.write( "|------|---------|-------|" );
				writer.newLine();
				for ( Number code : page.getCodingToCount().keySet() ) {
					writer.write( "| "+code );
					writer.write( " | "+page.getCodingToMeaning().get( code ) );
					writer.write( " | "+page.getCodingToCount().get( code )+" |" );
					writer.newLine();
				}
				writer.newLine();
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private static String getMainClassName() {
		// Not protected against strange things like methods named "main", but enough for current purpose
		for ( StackTraceElement[] trace : Thread.getAllStackTraces().values() ) {
			for ( StackTraceElement element : trace ) {
				if ( element.getMethodName().equals( "main" ) ) {
					return element.getClassName();
				}
			}
		}

		return "unknown";
	}
}
