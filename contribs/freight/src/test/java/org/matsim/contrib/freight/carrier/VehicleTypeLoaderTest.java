/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import javax.measure.unit.SystemOfUnits;

/**
 *  Test for using the usual vehicles / vehicle types (v2) format.
 *
 * @author kturner
 *  */
public class VehicleTypeLoaderTest extends MatsimTestCase{

	Vehicles vehicles;

	Carriers carriers;
	
	@Override
	public void setUp() throws Exception{
		super.setUp();

		System.setProperty("matsim.preferLocalDtds","true");

		vehicles = VehicleUtils.createVehiclesContainer();
//		new MatsimVehicleReader(vehicles).readFile(getClassInputDirectory() + "vehicles.xml");
		MatsimVehicleReader reader = new MatsimVehicleReader(vehicles);
		reader.readFile(this.getClassInputDirectory() + "vehicles.xml");

		carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).readFile(this.getClassInputDirectory() + "carrierPlansEquils.xml");
	}

	public void test_whenLoadingTypes_allAssignmentsInLightVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicles);
		CarrierVehicle v = getVehicle("lightVehicle");
		assertNotNull(v.getVehicleType());
		
	}
	
	public void test_whenLoadingTypes_allAssignmentsInMediumVehicleAreCorrectly(){
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(vehicles);
		CarrierVehicle v = getVehicle("mediumVehicle");
		assertNotNull(v.getVehicleType());
		
	}

	private CarrierVehicle getVehicle(String vehicleName) {
		for(CarrierVehicle v : carriers.getCarriers().get(Id.create("testCarrier", Carrier.class)).getCarrierCapabilities().getCarrierVehicles()){
			if(v.getVehicleId().toString().equals(vehicleName)){
				return v;
			}
		}
		return null;
	}
}
