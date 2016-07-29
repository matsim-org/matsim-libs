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
package playground.agarwalamit.mixedTraffic.patnaIndia.utils;

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
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.OuterCordonUtils.PatnaNetworkType;

/**
 * @author amit
 */

public final class PatnaUtils {

	public static final PatnaNetworkType PATNA_NETWORK_TYPE = PatnaNetworkType.shpNetwork;
	
	public static final String EPSG = "EPSG:24345";
	public static final CoordinateTransformation COORDINATE_TRANSFORMATION = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, PatnaUtils.EPSG);

	public static final String INPUT_FILES_DIR = "../../../../repos/shared-svn/projects/patnaIndia/inputs/";
	public static final String ZONE_FILE = PatnaUtils.INPUT_FILES_DIR+"/raw/others/wardFile/Wards.shp";

	public static final String INCOME_ATTRIBUTE = "monthlyIncome";
	public static final String TRANSPORT_COST_ATTRIBUTE = "dailyTransportCost";
	public static final String SUBPOP_ATTRIBUTE = "userGroup";
	
	public static final Double SLUM_AVG_INCOME = 3109.0; // sec 5.2.4, Patna CMP 
	public static final Double NONSLUM_AVG_INCOME = 7175.0; 
	
	public enum PatnaUrbanActivityTypes {
		home, work, educational, social, other, unknown;
	}

	public static final Collection <String> URBAN_MAIN_MODES = Arrays.asList("car","motorbike","bike");
	public static final Collection <String> URBAN_ALL_MODES = Arrays.asList("car","motorbike","bike","pt","walk");

	public static final Collection <String> EXT_MAIN_MODES = Arrays.asList("car_ext","motorbike_ext","bike_ext","truck_ext");

	public static final Collection <String> ALL_MAIN_MODES =  Arrays.asList("car","motorbike","bike","car_ext","motorbike_ext","bike_ext","truck_ext");
//			
	public static final Collection <String> ALL_MODES = Arrays.asList("car_ext","motorbike_ext","truck_ext","bike_ext","pt","walk","car","motorbike","bike");
	

	private PatnaUtils(){} 

	/**
	 * @param scenario
	 * It creates first vehicle types and add them to scenario and then create and add vehicles to the scenario.
	 */
	public static void createAndAddVehiclesToScenario(final Scenario scenario, final Collection <String> modes){
		final Map<String, VehicleType> modesType = new HashMap<String, VehicleType>(); 

		for (String mode : modes){
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

	public static int getAverageIncome(final String incomeCode){
		switch (incomeCode) {
		case "1": return 500;
		case "2": return 750;
		case "3": return 2000;
		case "4": return 4000;
		case "5": return 6250;
		case "6": return 8750;
		case "7": return 20000;
		default : throw new IllegalArgumentException("Unknown income code "+ incomeCode);
		}
	}
	
	public static double getAverageDailyTranportCost(final String dailyTranportCostCode){
		switch (dailyTranportCostCode) {
		case "1": return 10.00;
		case "2": return 17.50;
		case "3": return 37.5;
		case "4": return 75.0;
		case "5": return 200.0;
		default : throw new IllegalArgumentException("Unknown daily transport cost code "+ dailyTranportCostCode);
		}
	}

	public static int getIncomeIntervalCode(final double income){
		if (income < 500 ) return 1;
		else if(income < 1000) return 2;
		else if(income < 3000) return 3;
		else if(income < 5000) return 4;
		else if(income < 7500) return 5;
		else if(income < 10000) return 6;
		else return 7;
	}

	public static int getDailyTransportCost(final double dailyTransportCost){
		if (dailyTransportCost < 10 ) return 1;
		else if(dailyTransportCost < 25) return 2;
		else if(dailyTransportCost < 50) return 3;
		else if(dailyTransportCost < 100) return 4;
		else return 5;
	}
}