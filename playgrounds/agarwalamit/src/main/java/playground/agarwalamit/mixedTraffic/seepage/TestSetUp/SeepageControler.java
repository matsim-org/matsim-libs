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
package playground.agarwalamit.mixedTraffic.seepage.TestSetUp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * @author amit
 */
public class SeepageControler {
	 public static final String OUTPUT_DIR = "../../../repos/shared-svn/projects/mixedTraffic/seepage/xt_1Link/seepage/";
	 static final List<String> MAIN_MODES = Arrays.asList(TransportMode.car,TransportMode.bike);
	 static final String SEEP_MODE = "bike";
	 static final boolean IS_SEEP_MODE_STORAGE_FREE = false;
	 
	private void run (){
		CreateInputs inputs = new CreateInputs();
		inputs.run();
		Scenario sc = inputs.getScenario();
		
		sc.getConfig().qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		Map<String, VehicleType> modeVehicleTypes = new HashMap<>();

		for(String travelMode:MAIN_MODES){
			VehicleType mode = VehicleUtils.getFactory().createVehicleType(Id.create(travelMode,VehicleType.class));
			mode.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(travelMode));
			mode.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(travelMode));
			modeVehicleTypes.put(travelMode, mode);
			sc.getVehicles().addVehicleType(mode);
		}
		
		for(Person p:sc.getPopulation().getPersons().values()){
			Id<Vehicle> vehicleId = Id.create(p.getId(),Vehicle.class);
			String travelMode = null;
			for(PlanElement pe :p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Leg) {
					travelMode = ((Leg)pe).getMode();
					break;
				}
			}
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId,modeVehicleTypes.get(travelMode));
			sc.getVehicles().addVehicle(vehicle);
		}
		
		EventsManager manager = EventsUtils.createEventsManager();
		EventWriterXML eventWriterXML = new EventWriterXML(OUTPUT_DIR+"/events.xml");
		manager.addHandler(eventWriterXML);
		
		QSim qSim = QSimUtils.createDefaultQSim(sc, manager);
		qSim.run();
		eventWriterXML.closeFile();
		
//		new QPositionDataWriterForR().run();
//		new TravelTimeAnalyzer(outputDir).run();
	}
	
	public static void main(String[] args) {
		new SeepageControler().run();
	}
}
