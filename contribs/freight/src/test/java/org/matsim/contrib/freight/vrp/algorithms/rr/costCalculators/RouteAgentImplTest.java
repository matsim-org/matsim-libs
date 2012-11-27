package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.matsim.contrib.freight.vrp.basics.Coordinate;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.InsertionData;
import org.matsim.contrib.freight.vrp.basics.Shipment;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleFleetManager;
import org.matsim.contrib.freight.vrp.basics.VehicleFleetManagerImpl;
import org.matsim.contrib.freight.vrp.basics.VehicleImpl;
import org.matsim.contrib.freight.vrp.basics.InsertionData.NoInsertionFound;
import org.matsim.contrib.freight.vrp.basics.VehicleImpl.VehicleCostParams;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.utils.ManhattanDistanceCalculator;
import org.matsim.contrib.freight.vrp.utils.VrpTourBuilder;
import org.matsim.contrib.freight.vrp.utils.VrpUtils;

public class RouteAgentImplTest {
	
	private RouteAgentFactory routeAgentFactory;
	
	private TourImpl tour;
	
	private TourImpl emptyTour;
	
	private VehicleRoute emptyRoute;
	
	private VehicleRoute route;
	
	private VehicleFleetManager fleetManager;
	
	Vehicle heavyVehicle;
	
	Vehicle lightVehicle;
	
	Vehicle noVehicle;
	
	Shipment firstShipment;
	
	@Before
	public void setup(){
		
		VehicleRoutingCosts cost = new VehicleRoutingCosts() {
			
			@Override
			public double getBackwardTransportTime(String fromId, String toId,
					double arrivalTime, Driver driver, Vehicle vehicle) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public double getBackwardTransportCost(String fromId, String toId,
					double arrivalTime, Driver driver, Vehicle vehicle) {
				// TODO Auto-generated method stub
				return 0;
			}
			
			@Override
			public double getTransportCost(String fromId, String toId, double departureTime, Driver driver, Vehicle vehicle) {
				String[] fromTokens = fromId.split(",");
				String[] toTokens = toId.split(",");
				double fromX = Double.parseDouble(fromTokens[0]);
				double fromY = Double.parseDouble(fromTokens[1]);
				
				double toX = Double.parseDouble(toTokens[0]);
				double toY = Double.parseDouble(toTokens[1]);
				
				return vehicle.getType().vehicleCostParams.perDistanceUnit*ManhattanDistanceCalculator.calculateDistance(new Coordinate(fromX, fromY), new Coordinate(toX, toY));
			}
			
			@Override
			public double getTransportTime(String fromId, String toId, double departureTime, Driver driver, Vehicle vehicle) {
				return 0;
			}
		};
		
		firstShipment = getShipment("0,0","10,0");
		
		Shipment thirdShipment = getShipment("10,10","0,10");
		
		VrpTourBuilder tourBuilder = new VrpTourBuilder();
		tourBuilder.scheduleStart("0,0", 0.0, Double.MAX_VALUE);
		tourBuilder.schedulePickup(firstShipment);
		tourBuilder.scheduleDelivery(firstShipment);
		tourBuilder.scheduleEnd("0,0", 0.0, Double.MAX_VALUE);
		tour = tourBuilder.build();
		
		VrpTourBuilder emptyTourBuilder = new VrpTourBuilder();
		
		emptyTourBuilder.scheduleStart("0,0", 0.0, Double.MAX_VALUE);
		emptyTourBuilder.scheduleEnd("0,0", 0.0, Double.MAX_VALUE);
		
		emptyTour = tourBuilder.build();
		
		VehicleCostParams lightParams = VehicleImpl.getFactory().createVehicleCostParams(1.0, 0.0, 1.0);
		VehicleCostParams heavyParams = VehicleImpl.getFactory().createVehicleCostParams(2.0, 0.0, 2.0);
		lightVehicle = VehicleImpl.getFactory().createVehicle("light", "0,0", VehicleImpl.getFactory().createType("light", 10, lightParams));
		heavyVehicle = VehicleImpl.getFactory().createVehicle("heavy", "0,0", VehicleImpl.getFactory().createType("heavy", 10, heavyParams));
		
		noVehicle = new VehicleImpl.NoVehicle();
		
		CalculatesCostAndTWs tourStateCalculator = new CalculatesCostAndTWs(cost);
		tourStateCalculator.calculate(tour, heavyVehicle, new Driver(){});
		route = new VehicleRoute(tour, new Driver(){}, heavyVehicle);
		
		emptyRoute = new VehicleRoute(emptyTour, new Driver(){}, VehicleImpl.createNoVehicle());
		
		routeAgentFactory = new StandardRouteAgentFactory(new CalculatesShipmentInsertion(cost, new CalculatesLocalActInsertion(cost)), 
				tourStateCalculator);
		
		Collection<Vehicle> vehicles = new ArrayList<Vehicle>();
		vehicles.add(heavyVehicle);
		vehicles.add(lightVehicle);
		
		fleetManager = new VehicleFleetManagerImpl(vehicles);
		
	}
	
	@Test
	public void whenRouteAgent_hasDefaultVehicleFleetManagerAndNoVehicle_calculateBestInsertionReturnsNoInsertionDataFound(){
		RouteAgent agent = routeAgentFactory.createAgent(emptyRoute);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		assertTrue(iData instanceof NoInsertionFound);
	}
	
	@Test
	public void whenRouteAgent_canSelectSeveralVehicles_returnIDataWithCheapestVehicle(){
		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		RouteAgent agent = routeAgentFactory.createAgent(route);
		fleetManager.lock(heavyVehicle);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		assertEquals(lightVehicle, iData.getSelectedVehicle());
	}
	
	@Test
	public void whenRouteAgent_canSelectSeveralVehiclesAndHasNoVehicle_returnIDataWithCheapestVehicle(){
		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		RouteAgent agent = routeAgentFactory.createAgent(emptyRoute);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		assertEquals(lightVehicle, iData.getSelectedVehicle());
	}
	
	@Test
	public void whenRouteAgent_canSelectSeveralNoVehiclesAndHasNoVehicle_returnNoInsertionFound(){
//		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		RouteAgent agent = routeAgentFactory.createAgent(emptyRoute);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		assertTrue(iData instanceof NoInsertionFound);
	}
	
	@Test
	public void whenRouteAgent_initiallyHaveNullVehicle_handleItAsIfItWereANoVehicle(){
//		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		emptyRoute.setVehicle(null);
		RouteAgent agent = routeAgentFactory.createAgent(emptyRoute);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		assertTrue(iData instanceof NoInsertionFound);
	}
	
	@Test
	public void whenRouteAgent_mustRemoveNonExistingJob_doNotRemoveAnything(){
		RouteAgent agent = routeAgentFactory.createAgent(route);
		Shipment job = getShipment("0,0","10,10");
		int nOfActBefore = route.getTour().getActivities().size();
		agent.removeJobWithoutTourUpdate(job);
		int nOfActAfter = route.getTour().getActivities().size();
		assertEquals(nOfActAfter,nOfActBefore);
	}
	
	@Test
	public void whenRouteAgent_mustRemoveExistingJob_removeIt(){
		RouteAgent agent = routeAgentFactory.createAgent(route);
		int nOfActBefore = route.getTour().getActivities().size();
		agent.removeJobWithoutTourUpdate(firstShipment);
		int nOfActAfter = route.getTour().getActivities().size();
		assertEquals(nOfActAfter+2,nOfActBefore);
	}
	
	@Test
	public void whenRouteAgent_mustInsertJob_insertItWithIDataInformation(){
		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		RouteAgent agent = routeAgentFactory.createAgent(route);
		fleetManager.lock(heavyVehicle);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		int nOfActBefore = route.getTour().getActivities().size();
		agent.insertJobWithoutTourUpdate(job, iData);
		int nOfActAfter = route.getTour().getActivities().size();
		assertEquals(nOfActAfter-2, nOfActBefore);
	}
	
	@Test(expected=IllegalStateException.class)
	public void whenRouteAgent_mustInsertJobAndIDataInformationAreInValid_throwException(){
		RouteAgent agent = routeAgentFactory.createAgent(route);
		Shipment job = getShipment("0,0","10,10");
		agent.insertJobWithoutTourUpdate(job, new InsertionData(2.0, new int[]{6,7}));
		assertTrue(false);
	}
	
	@Test
	public void whenRouteAgent_mustInsertJobAndSetANewVehicle_newVehicleMustBeSet(){
		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		RouteAgent agent = routeAgentFactory.createAgent(route);
		fleetManager.lock(heavyVehicle);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		agent.insertJobWithoutTourUpdate(job, iData);
		assertEquals(lightVehicle, route.getVehicle());
	}
	
	@Test
	public void whenRouteAgent_mustInsertJobInEmptyRouteAndSetANewVehicle_newVehicleMustBeSet(){
		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		RouteAgent agent = routeAgentFactory.createAgent(emptyRoute);
		fleetManager.lock(heavyVehicle);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		agent.insertJobWithoutTourUpdate(job, iData);
		assertEquals(lightVehicle, emptyRoute.getVehicle());
	}
	
	@Test
	public void whenRouteAgent_mustInsertJobAndSetANewVehicle_lockNewAndUnlockOld(){
		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		RouteAgent agent = routeAgentFactory.createAgent(route);
		fleetManager.lock(heavyVehicle);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		agent.insertJobWithoutTourUpdate(job, iData);
//		assertEquals(lightVehicle, route.getVehicle());
		assertTrue(fleetManager.isLocked(lightVehicle));
		assertFalse(fleetManager.isLocked(heavyVehicle));
	}
	
	@Test(expected=IllegalStateException.class)
	public void whenRouteAgent_mustInsertJobAndSetALockedVehicle_throwException(){
		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		RouteAgent agent = routeAgentFactory.createAgent(route);
		fleetManager.lock(heavyVehicle);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		fleetManager.lock(lightVehicle);
		agent.insertJobWithoutTourUpdate(job, iData);
//		assertEquals(lightVehicle, route.getVehicle());
		assertTrue(false);
	}
	
	@Test
	public void whenRouteAgent_mustInsertJob_updataTourInformationAfterInsertion(){
		((StandardRouteAgentFactory)routeAgentFactory).setVehicleFleetManager(fleetManager);
		RouteAgent agent = routeAgentFactory.createAgent(route);
		fleetManager.lock(heavyVehicle);
		fleetManager.lock(lightVehicle);
		Shipment job = getShipment("0,0","10,10");
		InsertionData iData = agent.calculateBestInsertion(job, Double.MAX_VALUE);
		double tourCostBefore = route.getTour().tourData.transportCosts;
		agent.insertJob(job, iData);
		double tourCostAfter = route.getTour().tourData.transportCosts;
		assertTrue(tourCostBefore < tourCostAfter);
	}
	
	@Test(expected=IllegalStateException.class)
	public void whenRouteAgent_mustInsertJobWithNullInformation_throwException(){
		RouteAgent agent = routeAgentFactory.createAgent(route);
		Shipment job = getShipment("0,0","10,10");
		agent.insertJobWithoutTourUpdate(job, null);
		assertTrue(false);
	}
	
	@Test(expected=IllegalStateException.class)
	public void whenRouteAgent_mustInsertNullJob_throwException(){
		RouteAgent agent = routeAgentFactory.createAgent(route);
		Shipment job = getShipment("0,0","10,10");
		agent.insertJobWithoutTourUpdate(null, new NoInsertionFound());
		assertTrue(false);
	}

	private Shipment getShipment(String from, String to) {
		Shipment s = VrpUtils.createShipment(from+"_"+to, from, to, 0, VrpUtils.createTimeWindow(0.0, 20.0), VrpUtils.createTimeWindow(0.0, 20.0));
		return s;
	}

	private Shipment getShipment(String from, String to, double pickStart, double pickEnd, double delStart, double delEnd) {
		Shipment s = VrpUtils.createShipment("s", from, to, 0, VrpUtils.createTimeWindow(pickStart, pickEnd), VrpUtils.createTimeWindow(delStart, delEnd));
		return s;
	}
	
	
	

}
