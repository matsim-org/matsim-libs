/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;

import java.io.InputStream;
import java.net.URL;
import java.util.Stack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader that reads carriers and their plans.
 *
 * @author sschroeder
 *
 */
public class CarrierPlanXmlReader implements MatsimReader {
	private static final Logger log = LogManager.getLogger( CarrierPlanXmlReader.class );
	private static final String MSG="With early carrier plans file formats, there will be an expected exception in the following." ;

	private static final String CARRIERS = "carriers";

	private final CarriersPlanReader reader;

	public CarrierPlanXmlReader( final Carriers carriers, CarrierVehicleTypes carrierVehicleTypes ) {
		this.reader = new CarriersPlanReader( carriers, carrierVehicleTypes ) ;
	}

	@Override
	public void readFile( String filename ){
		log.info(MSG) ;
		try {
			reader.readFile( filename );
		} catch (Exception e) {
			log.warn("### Exception found while trying to read CarrierPlan: Message: {} ; cause: {} ; class {}", e.getMessage(), e.getCause(), e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
					log.warn("read with validation = true failed. Try it again without validation... filename: {}", filename);
					reader.setValidating(false);
					reader.readFile(filename);
			} else { //other problem: e.g. validation does not work, because of missing validation file.
			throw  e;}
		}
	}

	@Override
	public void readURL( URL url ){
		log.info(MSG) ;
		try {
			reader.readURL(url);
		}  catch (Exception e) {
			log.warn("### Exception found while trying to read CarrierPlan: Message: {} ; cause: {} ; class {}", e.getMessage(), e.getCause(), e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
				log.warn("read with validation = true failed. Try it again without validation... url: {}", url.toString());
			reader.setValidating(false);
			reader.readURL(url);
			} else { //other problem: e.g. validation does not work, because of missing validation file.
				throw  e;
			}
		}
	}

	public void readStream( InputStream inputStream ){
		log.info(MSG) ;
		try {
			reader.setValidating(false);
			reader.parse(inputStream);
		} catch (Exception e) {
			log.warn("### Exception found while trying to read CarrierPlan: Message: {} ; cause: {} ; class {}", e.getMessage(), e.getCause(), e.getClass());
			throw  e;
		}
	}

	private static final class CarriersPlanReader extends MatsimXmlParser {
		private final Carriers carriers;
		private final CarrierVehicleTypes carrierVehicleTypes;

		private MatsimXmlParser delegate = null;

		CarriersPlanReader( Carriers carriers, CarrierVehicleTypes carrierVehicleTypes ) {
			super(ValidationType.XSD_ONLY);
			this.carriers = carriers;
			this.carrierVehicleTypes = carrierVehicleTypes;
		}

		@Override
		public void startTag(final String name, final Attributes attributes, final Stack<String> context) {
			if ( CARRIERS.equalsIgnoreCase( name ) ) {
				String str = attributes.getValue( "xsi:schemaLocation" );
				log.info("Found following schemeLocation in carriers definition file: {}", str);
				if (str == null){
					log.warn("Carrier plans file does not contain a valid xsd header. Using CarrierPlanReaderV2.");
					delegate = new CarrierPlanXmlParserV2( carriers, carrierVehicleTypes ) ;
				} else if ( str.contains( "carriersDefinitions_v1.0.xsd" ) ){
					log.info("Found carriersDefinitions_v1.0.xsd. Using CarrierPlanReaderV1.");
					delegate = new CarrierPlanReaderV1(carriers, carrierVehicleTypes );
				} else if ( str.contains( "carriersDefinitions_v2.0.xsd" ) ) {
					log.info("Found carriersDefinitions_v2.0.xsd. Using CarrierPlanReaderV2.");
					delegate = new CarrierPlanXmlParserV2( carriers, carrierVehicleTypes );
				} else if ( str.contains( "carriersDefinitions_v2.1.xsd" ) ) {
					log.info("Found carriersDefinitions_v2.1.xsd. Using CarrierPlanReaderV2.1");
					delegate = new CarrierPlanXmlParserV2_1( carriers, carrierVehicleTypes );
				}else {
					throw new RuntimeException("no reader found for " + str ) ;
				}
			} else{
				this.delegate.startTag( name, attributes, context );
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
	}
}
