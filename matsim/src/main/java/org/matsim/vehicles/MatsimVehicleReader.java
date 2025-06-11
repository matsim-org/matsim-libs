/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author dgrether
 */
public final class MatsimVehicleReader implements MatsimReader{
	private final static Logger log = LogManager.getLogger( MatsimVehicleReader.class );
	private final VehicleReader reader;

	public MatsimVehicleReader( final Vehicles vehicles ) {
		this.reader = new VehicleReader( vehicles ) ;
	}

	@Override
	public void readFile( String filename ){
		reader.readFile( filename );
	}

	@Override
	public void readURL( URL url ){
		reader.readURL( url );
	}

	public void readStream( InputStream inputStream ){
		reader.parse( inputStream ) ;
	}

	public static final class VehicleReader extends MatsimXmlParser {
		private final Vehicles vehicles;

		private MatsimXmlParser delegate = null;

		private Map<Class<?>, AttributeConverter<?>> converters = new HashMap<>();

		public VehicleReader( Vehicles vehicles ) {
			super(ValidationType.XSD_ONLY);
			this.vehicles = vehicles;
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			if ( VehicleSchemaV1Names.VEHICLEDEFINITIONS.equalsIgnoreCase( name ) ) {
				String str = atts.getValue( "xsi:schemaLocation" );
				log.info("Found following schemaLocation in vehicle definition file: " + str);
				if ( str.contains( "vehicleDefinitions_v1.0.xsd" ) ){
					delegate = new VehicleReaderV1( vehicles );
				} else if ( str.contains( "vehicleDefinitions_v2.0.xsd" ) ) {
					delegate = new VehicleReaderV2( vehicles ) ;
				} else {
					throw new RuntimeException("no reader found for " + str ) ;
				}
			} else{
				if (this.delegate == null) {
					log.error("found no valid. vehicle definition file. this may happen if you try to parse a dvrp vehicles file for example or if no vehicle definition file is provided.\n" +
						"Valid vehicle definitions are at http://www.matsim.org/files/dtd/vehicleDefinitions_v2.0.xsd and http://www.matsim.org/files/dtd/vehicleDefinitions_v1.0.xsd\n" +
						"The code will crash with a NullPointerException.");
				}
				this.delegate.startTag( name, atts, context );
			}
		}

		@Override
		public void endTag(final String name, final String content, final Stack<String> context) {
			this.delegate.endTag(name, content, context);
		}

		@Override
		public void endDocument() {
			try {
				this.delegate.endDocument();
			} catch ( SAXException e) {
				throw new RuntimeException(e);
			}
		}

		public void putAttributeConverter(Class<?> clazz, AttributeConverter<?> converter) {
			this.converters.put( clazz, converter );
		}

		public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> attributeConverters) {
			this.converters.putAll( attributeConverters );
		}
	}


}
