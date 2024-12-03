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
import java.util.Map;
import java.util.Stack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Reader reading carrierVehicleTypes from a xml-file.
 *
 * @author sschroeder
 *
 */
public class CarrierVehicleTypeReader implements MatsimReader{
	private static final Logger log = LogManager.getLogger(CarrierVehicleTypeReader.class);
	private static final String MSG="With early carrier vehicle type file formats, there will be an expected exception in the following." ;
	private final CarrierVehicleTypeParser reader;

	public CarrierVehicleTypeReader( final CarrierVehicleTypes types ) {
		this.reader = new CarrierVehicleTypeParser( types ) ;
	}

	@Override
	public void readFile( String filename ){
		log.info(MSG) ;
		try {
			reader.setValidating(true) ;
			reader.readFile( filename );
		} catch (Exception e) {
			log.warn("### Exception: Message={} ; cause={} ; class={}", e.getMessage(), e.getCause(), e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
				log.warn("read with validation = true failed. Try it again without validation. filename: {}", filename);
				reader.setValidating(false);
				reader.readFile(filename);
			} else { //other problem: e.g. validation does not work, because of missing validation file.
				throw  e;
			}
		}
	}

	@Override
	public void readURL( URL url ){
		log.info(MSG) ;
		try {
			reader.setValidating(true) ;
			reader.readURL(url);
		}  catch (Exception e) {
			log.warn("### Exception: Message={}", e.getMessage());
			log.warn("### Exception: Cause={}", e.getCause().toString());
			log.warn("### Exception: Class={}", e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1.a")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
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
			reader.setValidating(true) ;
			reader.parse( inputStream ) ;
		} catch (Exception e)
		{
			log.warn("### Exception found while trying to read Carrier Vehicle Type: Message: {} ; cause: {} ; class {}", e.getMessage(), e.getCause(), e.getClass());
			if (e.getCause().getMessage().contains("cvc-elt.1.a")) { // "Cannot find the declaration of element" -> exception comes most probably because no validation information was found
				log.warn("read with validation = true failed. Try it again without validation... ");
				reader.setValidating(false);
				reader.parse(inputStream);
			} else { //other problem: e.g. validation does not work, because of missing validation file.
				throw  e;
			}
		}
	}

	private static final class CarrierVehicleTypeParser extends MatsimXmlParser {

		private final CarrierVehicleTypes vehicleTypes;
		private Vehicles vehicles = null ;
		private MatsimXmlParser delegate = null;

		CarrierVehicleTypeParser(CarrierVehicleTypes vehicleTypes) {
			super(ValidationType.XSD_ONLY);
			this.vehicleTypes = vehicleTypes;
		}

		@Override
		public void startTag(final String name, final Attributes attributes, final Stack<String> context) {
			log.debug("Reading start tag. name: {} , attributes: {} , context: {}", name, attributes.toString(), context);
			if ( "vehicleTypes".equalsIgnoreCase( name ) ) {
				String str = attributes.getValue( "xsi:schemaLocation" );
				log.info("Found following schemeLocation in carriers definition file: {}", str);
				if (str == null){
					log.warn( "No validation information found. Using ReaderV1." );
					delegate = new CarrierVehicleTypeReaderV1( vehicleTypes );
				} else{
					throw new RuntimeException( "should not happen" ) ;
				}
			} else if ( "vehicleDefinitions".equalsIgnoreCase( name ) ){
				String str = attributes.getValue( "xsi:schemaLocation" );
				if ( str==null ){
					throw new RuntimeException( "should not happen" );
				} else {
					log.info("Using central vehicle parser") ;
					vehicles = VehicleUtils.createVehiclesContainer();
					delegate = new MatsimVehicleReader.VehicleReader(vehicles) ;
					// only takes a vehicle container as argument :-(
				}

				// Note that if it is a v1 file, it starts with
				// <vehicleTypes>.  If it is a later file, it starts with <vehicleDefinitions>. kai, sep'19

			}
			this.delegate.startTag( name, attributes, context );
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
			if ( vehicles != null ) {
				for( Map.Entry<Id<VehicleType>, VehicleType> entry : vehicles.getVehicleTypes().entrySet() ){
					vehicleTypes.getVehicleTypes().put( entry.getKey(), entry.getValue() ) ;
					// need to copy from vehicles container to provided vehicle types :-(
				}
			}
		}
	}

}
