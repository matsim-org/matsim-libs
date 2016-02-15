/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.internalizationPt;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author ikaddoura
 *
 */
public class InVehicleDelayHandlerTest extends MatsimTestCase {

	private Id<Vehicle> vehicleId1 = Id.create("vehicleId1", Vehicle.class);
//	private Id<Vehicle> vehicleId2 = Id.create("vehicleId2", Vehicle.class);
	private Id<Person> ptDriverId1 = Id.create("driverId1", Person.class);
//	private Id<Person> ptDriverId2 = Id.create("driverId2", Person.class);
	private Id<Person> testAgent1 = Id.create("testAgent1", Person.class);
	private Id<Person> testAgent2 = Id.create("testAgent2", Person.class);
	private Id<Person> testAgent3 = Id.create("testAgent3", Person.class);

	
	List<TransferDelayInVehicleEvent> inVehDelayEvents;

	
	// three agents entering and leaving
	@Test
	public final void testWaitingDelayHandler1(){
		
		System.out.println("############################################################################################################");
		
		Config config = ConfigUtils.createConfig();
		MutableScenario scenario = (MutableScenario)(ScenarioUtils.createScenario(config));
		EventsManager events = EventsUtils.createEventsManager();
		config.transit().setUseTransit(true);
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(this.getClassInputDirectory() + "vehicles.xml");
			
		events.addHandler(new TransferDelayInVehicleEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(TransferDelayInVehicleEvent event) {
				inVehDelayEvents.add(event);
			}
			
		});
		
		this.inVehDelayEvents = new ArrayList<TransferDelayInVehicleEvent>();
		
		// Bus1 fährt los
		TransferDelayInVehicleHandler inVehDelayHandler = new TransferDelayInVehicleHandler(events, scenario);
		inVehDelayHandler.handleEvent(new TransitDriverStartsEvent(0, ptDriverId1, vehicleId1, null, null, null));
		// Bus1 kommt an und Person1 steigt in den leeren Bus.
		inVehDelayHandler.handleEvent(new VehicleArrivesAtFacilityEvent(1, vehicleId1, null, 0.));
		inVehDelayHandler.handleEvent(new PersonEntersVehicleEvent(2, testAgent1, vehicleId1));
		// Person2 steigt in Bus1, in dem eine Person sitzt
		inVehDelayHandler.handleEvent(new PersonEntersVehicleEvent(2, testAgent2, vehicleId1));
		// Person3 steigt in Bus1, in dem zwei Personen sitzen
		inVehDelayHandler.handleEvent(new PersonEntersVehicleEvent(2, testAgent3, vehicleId1));
		// Bus1 fährt los, dadurch werden die extra delay events geworfen
		inVehDelayHandler.handleEvent(new VehicleDepartsAtFacilityEvent(3, vehicleId1, null, 0.));
		
		// Bus1 kommt an und Person1 steigt aus Bus1, in dem 2 Personen sitzen.
		inVehDelayHandler.handleEvent(new VehicleArrivesAtFacilityEvent(5, vehicleId1, null, 0.));
		inVehDelayHandler.handleEvent(new PersonLeavesVehicleEvent(6, testAgent1, vehicleId1));
		// Person 2 steigt aus Bus1, in dem eine Person sitzt
		inVehDelayHandler.handleEvent(new PersonLeavesVehicleEvent(6, testAgent2, vehicleId1));
		// Person 3 steigt aus Bus1, in dem keiner mehr sitzt
		inVehDelayHandler.handleEvent(new PersonLeavesVehicleEvent(6, testAgent3, vehicleId1));
		// Bus1 fährt los, dadurch werden die extra delay events geworfen
		inVehDelayHandler.handleEvent(new VehicleDepartsAtFacilityEvent(7, vehicleId1, null, 0.));
		
		for (TransferDelayInVehicleEvent delay : this.inVehDelayEvents){

			 if (delay.getTime()==2.0){
				// boarding delays
				
				// Person1, Bus1
				if (delay.getCausingAgent().toString().equals(this.testAgent1.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getAccessTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
				
				// Person2, Bus1
				else if (delay.getCausingAgent().toString().equals(this.testAgent2.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getAccessTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 1, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
				
				// Person3, Bus1
				else if (delay.getCausingAgent().toString().equals(this.testAgent3.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getAccessTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 2, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
			}
			
			else if (delay.getTime()==3.0){
				// extra delays boarding
				double delayPerPerson = 2.0 / 3;
				// Person1, Bus1
				if (delay.getCausingAgent().toString().equals(this.testAgent1.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
				
				// Person2, Bus1
				else if (delay.getCausingAgent().toString().equals(this.testAgent2.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
				
				// Person3, Bus1
				else if (delay.getCausingAgent().toString().equals(this.testAgent3.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
			}
			 
			 
			else if (delay.getTime()==6.0){
				// alighting delays
				
				// Person1, Bus1
				if (delay.getCausingAgent().toString().equals(this.testAgent1.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getEgressTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 2, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
				
				// Person2, Bus1
				else if (delay.getCausingAgent().toString().equals(this.testAgent2.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getEgressTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 1, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
				
				// Person3, Bus1
				else if (delay.getCausingAgent().toString().equals(this.testAgent3.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getEgressTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
			}
			 
			else if (delay.getTime()==7.0){
				// extra delays alighting
				double delayPerPerson = 2.0 / 3;
				// Person1, Bus1
				if (delay.getCausingAgent().toString().equals(this.testAgent1.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
				
				// Person2, Bus1
				else if (delay.getCausingAgent().toString().equals(this.testAgent2.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
				
				// Person3, Bus1
				else if (delay.getCausingAgent().toString().equals(this.testAgent3.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
					Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
					Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
				}
			}
			 
		}
		
	}
	
	
	// three agents entering and 2 agents leaving
		@Test
		public final void testWaitingDelayHandler2(){
			
			System.out.println("############################################################################################################");
			
			Config config = ConfigUtils.createConfig();;
			MutableScenario scenario = (MutableScenario)(ScenarioUtils.createScenario(config));
			EventsManager events = EventsUtils.createEventsManager();
			config.transit().setUseTransit(true);
			new VehicleReaderV1(scenario.getTransitVehicles()).readFile(this.getClassInputDirectory() + "vehicles.xml");
				
			events.addHandler(new TransferDelayInVehicleEventHandler() {

				@Override
				public void reset(int iteration) {				
				}

				@Override
				public void handleEvent(TransferDelayInVehicleEvent event) {
					inVehDelayEvents.add(event);
				}
				
			});
			
			this.inVehDelayEvents = new ArrayList<TransferDelayInVehicleEvent>();
			
			// Bus1 fährt los
			TransferDelayInVehicleHandler inVehDelayHandler = new TransferDelayInVehicleHandler(events, scenario);
			inVehDelayHandler.handleEvent(new TransitDriverStartsEvent(0, ptDriverId1, vehicleId1, null, null, null));
			// Bus1 kommt an und Person1 steigt in den leeren Bus.
			inVehDelayHandler.handleEvent(new VehicleArrivesAtFacilityEvent(1, vehicleId1, null, 0.));
			inVehDelayHandler.handleEvent(new PersonEntersVehicleEvent(2, testAgent1, vehicleId1));
			// Person2 steigt in Bus1, in dem eine Person sitzt
			inVehDelayHandler.handleEvent(new PersonEntersVehicleEvent(2, testAgent2, vehicleId1));
			// Person3 steigt in Bus1, in dem zwei Personen sitzen
			inVehDelayHandler.handleEvent(new PersonEntersVehicleEvent(2, testAgent3, vehicleId1));
			// Bus1 fährt los, dadurch werden die extra delay events geworfen
			inVehDelayHandler.handleEvent(new VehicleDepartsAtFacilityEvent(3, vehicleId1, null, 0.));
			
			// Bus1 kommt an und Person1 steigt aus Bus1, in dem 2 Personen sitzen.
			inVehDelayHandler.handleEvent(new VehicleArrivesAtFacilityEvent(5, vehicleId1, null, 0.));
			inVehDelayHandler.handleEvent(new PersonLeavesVehicleEvent(6, testAgent1, vehicleId1));
			// Person 2 steigt aus Bus1, in dem eine Person sitzt
			inVehDelayHandler.handleEvent(new PersonLeavesVehicleEvent(6, testAgent2, vehicleId1));
			// Bus1 fährt los, dadurch werden die extra delay events geworfen
			inVehDelayHandler.handleEvent(new VehicleDepartsAtFacilityEvent(7, vehicleId1, null, 0.));
			
			for (TransferDelayInVehicleEvent delay : this.inVehDelayEvents){

				 if (delay.getTime()==2.0){
					// boarding delays
					
					// Person1, Bus1
					if (delay.getCausingAgent().toString().equals(this.testAgent1.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
						Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getAccessTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
						Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
					
					// Person2, Bus1
					else if (delay.getCausingAgent().toString().equals(this.testAgent2.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
						Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getAccessTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
						Assert.assertEquals("affected agents", 1, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
					
					// Person3, Bus1
					else if (delay.getCausingAgent().toString().equals(this.testAgent3.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
						Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getAccessTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
						Assert.assertEquals("affected agents", 2, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
				}
				
				else if (delay.getTime()==3.0){
					// extra delays boarding
					double delayPerPerson = 2.0 / 3;
					// Person1, Bus1
					if (delay.getCausingAgent().toString().equals(this.testAgent1.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
						Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
						Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
					
					// Person2, Bus1
					else if (delay.getCausingAgent().toString().equals(this.testAgent2.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
						Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
						Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
					
					// Person3, Bus1
					else if (delay.getCausingAgent().toString().equals(this.testAgent3.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
						Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
						Assert.assertEquals("affected agents", 0, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
				}
				 
				 
				else if (delay.getTime()==6.0){
					// alighting delays
					
					// Person1, Bus1
					if (delay.getCausingAgent().toString().equals(this.testAgent1.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
						Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getEgressTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
						Assert.assertEquals("affected agents", 2, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
					
					// Person2, Bus1
					else if (delay.getCausingAgent().toString().equals(this.testAgent2.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
							Assert.assertEquals("delay", scenario.getTransitVehicles().getVehicles().get(vehicleId1).getType().getEgressTime(), delay.getDelay(), MatsimTestUtils.EPSILON);
							Assert.assertEquals("affected agents", 1, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
					else {
						System.out.println("unknown event");
					}
				}
				 
				else if (delay.getTime()==7.0){
					// extra delays alighting
					double delayPerPerson = 2.0 / 2;
					
					// Person1, Bus1
					if (delay.getCausingAgent().toString().equals(this.testAgent1.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
						Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
						Assert.assertEquals("affected agents", 1, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
					
					// Person2, Bus1
					else if (delay.getCausingAgent().toString().equals(this.testAgent2.toString()) && delay.getVehicleId().toString().equals(this.vehicleId1.toString())){
						Assert.assertEquals("delay", delayPerPerson, delay.getDelay(), MatsimTestUtils.EPSILON);
						Assert.assertEquals("affected agents", 1, delay.getAffectedAgents(), MatsimTestUtils.EPSILON);
					}
					
					else {
						System.out.println("Unknown event!");
					}
				}
				 
			}
			
		}
	
	
}
