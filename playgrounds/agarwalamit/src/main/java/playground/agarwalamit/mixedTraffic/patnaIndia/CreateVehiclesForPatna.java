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
package playground.agarwalamit.patnaIndia;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

/**
 * @author amit
 */
public class CreateVehiclesForPatna {


	public static void main(String[] args) {


		Vehicles vehicles = VehicleUtils.createVehiclesContainer();

		Map<String, VehicleType> modesType = new HashMap<String, VehicleType>(); 
		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
		car.setMaximumVelocity(60.0/3.6);
		car.setPcuEquivalents(1.0);
		modesType.put("car", car);

		VehicleType motorcycle = VehicleUtils.getFactory().createVehicleType(Id.create("motorbike",VehicleType.class));
		motorcycle.setMaximumVelocity(60.0/3.6);
		motorcycle.setPcuEquivalents(0.25);
		modesType.put("motorbike", motorcycle);

		VehicleType bicycle = VehicleUtils.getFactory().createVehicleType(Id.create("bike",VehicleType.class));
		bicycle.setMaximumVelocity(15.0/3.6);
		bicycle.setPcuEquivalents(0.25);
		modesType.put("bike", bicycle);

		VehicleType walk = VehicleUtils.getFactory().createVehicleType(Id.create("walk",VehicleType.class));
		walk.setMaximumVelocity(1.5);
		walk.setPcuEquivalents(0.10);  			// assumed pcu for walks is 0.1
		modesType.put("walk",walk);

		VehicleType pt = VehicleUtils.getFactory().createVehicleType(Id.create("pt",VehicleType.class));
		pt.setMaximumVelocity(40/3.6);
		pt.setPcuEquivalents(5);  			// assumed pcu for walks is 0.1
		modesType.put("pt",pt);

		vehicles.addVehicleType(car);
		vehicles.addVehicleType(motorcycle);
		vehicles.addVehicleType(bicycle);
		vehicles.addVehicleType(walk);
		vehicles.addVehicleType(pt);
		
		// I think, following lines are not necessary.
//		for(Person p : scenario.getPopulation().getPersons().values()){
//			for(PlanElement pe:p.getSelectedPlan().getPlanElements()) {
//				if(pe instanceof Leg ){
//					String travelMode =  ((Leg) pe).getMode();
//					if(!modesType.containsKey(travelMode)){
//						throw new RuntimeException("Vehicle Type is not defined. Define"+ travelMode+ "vehicle Type.");	
//					}
//
//					VehicleType vType = modesType.get(travelMode);
//					Vehicle veh =  VehicleUtils.getFactory().createVehicle(p.getId(), vType);
//					vehicles.addVehicle(veh);
//				}
//			}
//		}
		new VehicleWriterV1(vehicles).writeFile(MyFirstControler.outputDir+"/vehiclesPatna.xml");
	}
}
