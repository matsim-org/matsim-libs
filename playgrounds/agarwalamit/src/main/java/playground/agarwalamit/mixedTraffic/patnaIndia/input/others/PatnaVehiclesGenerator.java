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
package playground.agarwalamit.mixedTraffic.patnaIndia.input.others;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.vehicles.*;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class PatnaVehiclesGenerator {

	private final Scenario scenario ;
	private static double pcu_2w = Double.NaN;

//	public static void addVehiclesToScenarioFromVehicleFile(final String vehiclesFile, final Scenario scenario){
//		Vehicles vehs = VehicleUtils.createVehiclesContainer();
//		new VehicleReaderV1(vehs).readFile(vehiclesFile);
//		for(VehicleType vt : vehs.getVehicleTypes().values()) {
//			scenario.getVehicles().addVehicleType(vt);
//		}
//	}

	/**
	 * @param plansFile is required to get vehicles for each agent type in population.
	 * @param pcu_2w pcu for 2-wheeler if not using default from mixed traffic vehicle utils.
	 */
	public PatnaVehiclesGenerator(final String plansFile, final double pcu_2w) {
		this.scenario = LoadMyScenarios.loadScenarioFromPlans(plansFile);
		PatnaVehiclesGenerator.pcu_2w = pcu_2w;
	}
	
	/**
	 * @param plansFile is required to get vehicles for each agent type in population. The pcu of 2w is taken from mixed traffic vehicle utils.
	 */
	public PatnaVehiclesGenerator(final String plansFile) {
		this(plansFile, Double.NaN);
	}
	
	public Vehicles createAndReturnVehicles(final Collection <String> modes){
		PatnaVehiclesGenerator.createAndAddVehiclesToScenario(scenario, modes);
		return scenario.getVehicles();
	}
	
	/**
	 * @param scenario
	 * It creates first vehicle types and add them to scenario and then create and add vehicles to the scenario.
	 */
	public static void createAndAddVehiclesToScenario(final Scenario scenario, final Collection <String> modes){
		final Map<String, VehicleType> modesType = new HashMap<>();

		for (String mode : modes){
			VehicleType vehicle = VehicleUtils.getFactory().createVehicleType(Id.create(mode,VehicleType.class));
			vehicle.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(mode));
			
			if( Double.isNaN(pcu_2w) ) vehicle.setPcuEquivalents( MixedTrafficVehiclesUtils.getPCU(mode) );
			else vehicle.setPcuEquivalents( mode.equals("bike")||mode.equals("motorbike") ? pcu_2w : MixedTrafficVehiclesUtils.getPCU(mode) );
			
			modesType.put(mode, vehicle);
			scenario.getVehicles().addVehicleType(vehicle);
		}

		for(Person p:scenario.getPopulation().getPersons().values()){
			for(PlanElement pe :p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Leg) {
					String travelMode = ((Leg)pe).getMode();
					VehicleType vt = modesType.get(travelMode);
					if (vt!=null) {
						Id<Vehicle> vehicleId = Id.create(p.getId(),Vehicle.class);
						Vehicle veh = VehicleUtils.getFactory().createVehicle(vehicleId, vt);
						scenario.getVehicles().addVehicle(veh);
						break;// all trips have same mode and once a vehicle is added just break for the person.
					}
				}
			}
		}
	}
}