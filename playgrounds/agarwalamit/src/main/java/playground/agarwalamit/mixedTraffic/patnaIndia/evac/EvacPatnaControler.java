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
package playground.agarwalamit.mixedTraffic.patnaIndia.evac;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.SeepageMobsimfactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;
import playground.ikaddoura.analysis.welfare.WelfareAnalysisControlerListener;

/**
 * @author amit
 */

public class EvacPatnaControler {
	
	final static boolean  isUsingSeepage = true;
	
	public static void main(String[] args) {
		
		EvacuationPatnaScenarioGenerator scenarioCreator = new EvacuationPatnaScenarioGenerator();
		scenarioCreator.run();
		Scenario sc = ScenarioUtils.loadScenario(scenarioCreator.getPatnaEvacConfig()); 
		
		sc.getConfig().qsim().setUseDefaultVehicles(false);
		((ScenarioImpl) sc).createVehicleContainer();

		Map<String, VehicleType> modesType = new HashMap<String, VehicleType>(); 
		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
		car.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("car"));
		car.setPcuEquivalents(1.0);
		modesType.put("car", car);
		sc.getVehicles().addVehicleType(car);

		VehicleType motorbike = VehicleUtils.getFactory().createVehicleType(Id.create("motorbike",VehicleType.class));
		motorbike.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("motorbike"));
		motorbike.setPcuEquivalents(0.25);
		modesType.put("motorbike", motorbike);
		sc.getVehicles().addVehicleType(motorbike);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike",VehicleType.class));
		bike.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("bike"));
		bike.setPcuEquivalents(0.25);
		modesType.put("bike", bike);
		sc.getVehicles().addVehicleType(bike);

//		VehicleType walk = VehicleUtils.getFactory().createVehicleType(Id.create("walk",VehicleType.class));
//		walk.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("walk"));
//		//		walk.setPcuEquivalents(0.10);  			
//		modesType.put("walk",walk);
//		sc.getVehicles().addVehicleType(walk);
//
//		VehicleType pt = VehicleUtils.getFactory().createVehicleType(Id.create("pt",VehicleType.class));
//		pt.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed("pt"));
//		//		pt.setPcuEquivalents(5);  			
//		modesType.put("pt",pt);
//		sc.getVehicles().addVehicleType(pt);

		for(Person p:sc.getPopulation().getPersons().values()){
			Id<Vehicle> vehicleId = Id.create(p.getId(),Vehicle.class);
			String travelMode = null;
			for(PlanElement pe :p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Leg) {
					travelMode = ((Leg)pe).getMode();
					break;
				}
			}
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId,modesType.get(travelMode));
			sc.getVehicles().addVehicle(vehicle);
		}
		
		final Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		controler.setDumpDataAtEnd(true);

		if(isUsingSeepage){
			controler.setMobsimFactory(new SeepageMobsimfactory());
		}
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl)controler.getScenario()));
		controler.run();
		
	}
}
