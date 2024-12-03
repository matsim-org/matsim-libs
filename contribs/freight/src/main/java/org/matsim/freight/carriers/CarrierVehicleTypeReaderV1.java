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

import java.util.Stack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.CostInformation;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.xml.sax.Attributes;

class CarrierVehicleTypeReaderV1 extends MatsimXmlParser {
	private static final Logger log = LogManager.getLogger(CarrierVehicleTypeReaderV1.class) ;

	private static int wrnCnt=10 ;

	private final CarrierVehicleTypes carrierVehicleTypes;
	private VehicleType currentType;

	CarrierVehicleTypeReaderV1( CarrierVehicleTypes carrierVehicleTypes ) {
		super(ValidationType.XSD_ONLY);
		this.carrierVehicleTypes = carrierVehicleTypes;
		this.setValidating(false);
	}


	@Override
	public void startTag( String name, Attributes attributes, Stack<String> context ) {
		if(name.equals("vehicleType")){
			Id<VehicleType> currentTypeId = Id.create( attributes.getValue( "id" ), VehicleType.class );
			this.currentType = VehicleUtils.getFactory().createVehicleType( currentTypeId ) ;
			// If no network mode is given, assume car, this is to be backwards compatible
			// The v2 format will not make this assumption, and the network mode will be required
			this.currentType.setNetworkMode(TransportMode.car);
		}
		if(name.equals("allowableWeight")){
//			String weight = atts.getValue("weight");
//			Double.parseDouble( weight );
			// yyyyyy what is this?  kai, sep'19
			if ( wrnCnt>0 ){
				log.warn( "allowableWeight is ignored (and has always been)." );
				wrnCnt--;
				if( wrnCnt == 0 ){
					log.warn( Gbl.FUTURE_SUPPRESSED );
				}
			}
		}
		if(name.equals("engineInformation")){
			EngineInformation engineInfo = this.currentType.getEngineInformation() ;
			VehicleUtils.setHbefaTechnology(engineInfo, attributes.getValue( "fuelType"));
			VehicleUtils.setFuelConsumptionLitersPerMeter(engineInfo, Double.parseDouble(attributes.getValue( "gasConsumption" )));
		}

		if(name.equals("costInformation")){
			String fix = attributes.getValue("fix");
			// yyyyyy shouldn't this be "perDay"??? kai, aug'19
			String perMeter = attributes.getValue("perMeter");
			String perSecond = attributes.getValue("perSecond");
			if(fix == null || perMeter == null || perSecond == null) throw new IllegalStateException("cannot read costInformation correctly. probably the paramName was written wrongly");
			CostInformation vehicleCosts = this.currentType.getCostInformation();
			vehicleCosts.setFixedCost( Double.valueOf( fix ) );
			vehicleCosts.setCostsPerMeter( Double.valueOf( perMeter ) );
			vehicleCosts.setCostsPerSecond( Double.valueOf( perSecond ) );
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals("description")){//Ca
			this.currentType.setDescription( content );
		}
		if(name.equals("capacity")){
			this.currentType.getCapacity().setOther( Double.parseDouble( content ) ) ;
		}
		if(name.equals("maxVelocity")){
			this.currentType.setMaximumVelocity( Double.parseDouble( content ) );
		}
		if(name.equals("vehicleType")){
			carrierVehicleTypes.getVehicleTypes().put(this.currentType.getId(), currentType );
			reset();
		}

	}

	private void reset() {
		currentType = null ;
	}

}
