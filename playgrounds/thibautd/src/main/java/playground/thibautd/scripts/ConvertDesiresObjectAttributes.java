/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertDesiresObjectAttributes.java
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
package playground.thibautd.scripts;

import playground.ivt.utils.Desires;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.thibautd.utils.DesiresConverter;
import playground.thibautd.utils.DesiresXmlLikeConverter;

/**
 * @author thibautd
 */
public class ConvertDesiresObjectAttributes {
	public static void main(final String[] args) {
		final String inputFile = args[ 0 ];
		final String outputFile = args[ 1  ];

		final ObjectAttributes attributes = new ObjectAttributes();
		final ObjectAttributesXmlReader reader =
			new ObjectAttributesXmlReader( attributes );
		reader.putAttributeConverter( Desires.class , new DesiresXmlLikeConverter() );
		reader.parse( inputFile );

		final ObjectAttributesXmlWriter writer =
			new ObjectAttributesXmlWriter( attributes );
		writer.putAttributeConverter( Desires.class , new DesiresConverter() );
		writer.writeFile( outputFile );

	}
}

