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
package playground.thibautd.utils;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.IOException;
import java.util.Map;

/**
 * @author thibautd
 */
public class PopulationToCsvConverter {
	public interface FieldExtractor {
		Map<String, String> getFields( Person p );
	}

	public static void convert(
			final Population population,
			final String csvFile,
			final FieldExtractor fields ) {
		final CsvUtils.TitleLine titleLine = new CsvUtils.TitleLine(
				fields.getFields(
						population.getPersons().values().stream().
								findAny().get() ).keySet() );

		try ( final CsvWriter writer = new CsvWriter( '\t' , '\"' , titleLine , csvFile ) ) {
			for ( Person p : population.getPersons().values() ) {
				final Map<String,String> fs = fields.getFields( p );
				writer.nextLine();
				for ( Map.Entry<String,String> e : fs.entrySet() ) {
					writer.setField( e.getKey() , e.getValue() );
				}
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}
}
