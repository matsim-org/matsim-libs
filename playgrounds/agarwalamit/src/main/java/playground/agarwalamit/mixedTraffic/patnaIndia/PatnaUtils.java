/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.agarwalamit.mixedTraffic.patnaIndia;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit
 */

public final class PatnaUtils {

	public static final CoordinateTransformation COORDINATE_TRANSFORMATION = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:24345");
	
	public static final String INPUT_FILES_DIR = "../../../../repos/shared-svn/projects/patnaIndia/inputs/";
	
	public enum PatnaActivityTypes {
		home, work, educational, social, other, unknown;
	}
	
	public static final Collection <String> MAIN_MODES = Arrays.asList("car","motorbike","bike");
	public static final Collection <String> ALL_MODES = Arrays.asList("car","motorbike","bike","pt","walk");
	
	private PatnaUtils(){} 
	
	/**
	 * @param scenario
	 * It creates first vehicle types and add them to scenario and then create and add vehicles to the scenario.
	 */
	public static void createAndAddVehiclesToScenario(final Scenario scenario){
		final Map<String, VehicleType> modesType = new HashMap<String, VehicleType>(); 
		
		for (String mode : PatnaUtils.ALL_MODES){
			VehicleType vehicle = VehicleUtils.getFactory().createVehicleType(Id.create(mode,VehicleType.class));
			vehicle.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(mode));
			vehicle.setPcuEquivalents( MixedTrafficVehiclesUtils.getPCU(mode) );
			modesType.put(mode, vehicle);
			scenario.getVehicles().addVehicleType(vehicle);
		}

		for(Person p:scenario.getPopulation().getPersons().values()){
			Id<Vehicle> vehicleId = Id.create(p.getId(),Vehicle.class);
			String travelMode = null;
			for(PlanElement pe :p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Leg) {
					travelMode = ((Leg)pe).getMode();
					break;
				}
			}
			final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId,modesType.get(travelMode));
			scenario.getVehicles().addVehicle(vehicle);
		}
	}
}