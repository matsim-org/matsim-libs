/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package org.matsim.contrib.freight.utils;

import java.util.Collection;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;

import org.jfree.util.Log;
import org.junit.Assert;

public class TestFreightUtils {
	
	private static final Id<Carrier> CARRIER_SERVICES_ID = Id.create("CarrierWServices", Carrier.class);
	private static final Id<Carrier> CARRIER_SHIPMENTS_ID = Id.create("CarrierWShipments", Carrier.class);
	
	private static Carriers carriersWithServicesAndShpiments;
	private static Carrier carrierWServices;
	private static Carrier carrierWShipments;
	
	private static Carriers carriersWithShipmentsOnly;
	private static Carrier carrierWShipmentsOnlyFromCarrierWServices;
	private static Carrier carrierWShipmentsOnlyFromCarrierWShipments;
	
//	@Rule
//	public static MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@BeforeClass
	public static void setUp() {
		
		//Create carrier with services and shipments
		carriersWithServicesAndShpiments = new Carriers() ;
		carrierWServices = CarrierImpl.newInstance(CARRIER_SERVICES_ID );
		carrierWServices.getServices().add(createMatsimService("Service1", "i(3,9)", 2));
		carrierWServices.getServices().add(createMatsimService("Service2", "i(4,9)", 2));
		
		//Create carrier with shipments
		carrierWShipments = CarrierImpl.newInstance(CARRIER_SHIPMENTS_ID);
		carrierWShipments.getShipments().add(createMatsimShipment("shipment1", "i(1,0)", "i(7,6)R", 1)); 
		carrierWShipments.getShipments().add(createMatsimShipment("shipment2", "i(3,0)", "i(3,7)", 2));

		//Create vehicle for Carriers
		CarrierVehicleType carrierVehType = CarrierVehicleType.Builder.newInstance(Id.create("gridType", VehicleType.class))
				.setCapacity(3)
				.setMaxVelocity(10)
				.setCostPerDistanceUnit(0.0001)
				.setCostPerTimeUnit(0.001)
				.setFixCost(130)
				.setEngineInformation(new EngineInformationImpl(FuelType.diesel, 0.015))
				.build();
		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);
		
		CarrierVehicle carrierVehicle = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(6,0)")).setEarliestStart(0.0).setLatestEnd(36000.0).setTypeId(carrierVehType.getId()).build();
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance() 
				.addType(carrierVehType)
				.addVehicle(carrierVehicle)
				.setFleetSize(FleetSize.INFINITE);				
		carrierWServices.setCarrierCapabilities(ccBuilder.build());
		carrierWShipments.setCarrierCapabilities(ccBuilder.build());

		// Add both carriers
		carriersWithServicesAndShpiments.addCarrier(carrierWServices);
		carriersWithServicesAndShpiments.addCarrier(carrierWShipments);

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriersWithServicesAndShpiments).loadVehicleTypes(vehicleTypes) ;	

		//load Network and build netbasedCosts for jsprit
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(getPackageInputDirectory() + "grid-network.xml"); 
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;
		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.
		

		//Build jsprit, solve and route VRP for carrierService only -> need solution to convert Services to Shipments
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrierWServices, network);
		vrpBuilder.setRoutingCost(netBasedCosts) ;
		VehicleRoutingProblem problem = vrpBuilder.build();

			// get the algorithm out-of-the-box, search solution and get the best one.
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

			//Routing bestPlan to Network
		CarrierPlan carrierPlanServicesAndShipments = MatsimJspritFactory.createPlan(carrierWServices, bestSolution) ;
		NetworkRouter.routePlan(carrierPlanServicesAndShipments,netBasedCosts) ;
		carrierWServices.setSelectedPlan(carrierPlanServicesAndShipments) ;

		/*
		 * Now convert it to a only shipment-based VRP.
		 */

		//Convert to jsprit VRP
		carriersWithShipmentsOnly = FreightUtils.createShipmentVRPCarrierFromServiceVRPSolution(carriersWithServicesAndShpiments);
		carrierWShipmentsOnlyFromCarrierWServices = carriersWithShipmentsOnly.getCarriers().get(CARRIER_SERVICES_ID);		//with converted Service
		carrierWShipmentsOnlyFromCarrierWShipments = carriersWithShipmentsOnly.getCarriers().get(CARRIER_SHIPMENTS_ID);		//with copied Shipments

		// assign vehicle types to the carriers
		new CarrierVehicleTypeLoader(carriersWithShipmentsOnly).loadVehicleTypes(vehicleTypes) ;	

//		VehicleRoutingProblem.Builder vrpBuilderShipmentsOnly = MatsimJspritFactory.createRoutingProblemBuilder(carrierWShipmentsOnlyFromCarrierWServices, network);
//
//		vrpBuilderShipmentsOnly.setRoutingCost(netBasedCosts) ;
//
//		VehicleRoutingProblem problemSchipmentsOnly = vrpBuilderShipmentsOnly.build();
//
//		// get the algorithm out-of-the-box, search solution and get the best one.
//		VehicleRoutingAlgorithm algorithmShipmentsOnly = new SchrimpfFactory().createAlgorithm(problemSchipmentsOnly);
//		Collection<VehicleRoutingProblemSolution> solutionsShipmentsOnly = algorithmShipmentsOnly.searchSolutions();
//		VehicleRoutingProblemSolution bestSolutionShipmentsOnly = Solutions.bestOf(solutionsShipmentsOnly);
//
//
//		CarrierPlan carrierPlanShipmentsOnly = MatsimJspritFactory.createPlan(carrierWShipmentsOnlyFromCarrierWServices, bestSolutionShipmentsOnly) ;
//		NetworkRouter.routePlan(carrierPlanShipmentsOnly,netBasedCosts) ;
//		carrierWShipmentsOnlyFromCarrierWServices.setSelectedPlan(carrierPlanShipmentsOnly) ;
	}
	


	@Test //Should only have Services
	public void numberOfInitalServicesIsCorrect() {
		Assert.assertEquals(2, carrierWServices.getServices().size());
		
		int demandServices = 0;
		for (CarrierService carrierService : carrierWServices.getServices()) {
			demandServices += carrierService.getCapacityDemand();
		}
		Assert.assertEquals(4, demandServices);
		
		Assert.assertEquals(0, carrierWServices.getShipments().size());
	}
	
	@Test //Should only have Shipments
	public void numberOfInitialShipmentsIsCorrect() {
		Assert.assertEquals(0, carrierWShipments.getServices().size());
		
		Assert.assertEquals(2, carrierWShipments.getShipments().size());
		int demandShipments = 0;
		for (CarrierShipment carrierShipment : carrierWShipments.getShipments()) {
			demandShipments += carrierShipment.getSize();
		}
		Assert.assertEquals(3, demandShipments);
	}
	
	@Test
	public void numberOfShipmentsFromCopiedShipmentsIsCorrect() {
		Assert.assertEquals(0, carrierWShipmentsOnlyFromCarrierWShipments.getServices().size());
		
		Assert.assertEquals(2, carrierWShipmentsOnlyFromCarrierWShipments.getShipments().size());
		int demandShipments = 0;
		for (CarrierShipment carrierShipment : carrierWShipmentsOnlyFromCarrierWServices.getShipments()) {
			demandShipments += carrierShipment.getSize();
		}
		Assert.assertEquals(4, demandShipments);
	}
	
	@Test
	public void numberOfShipmentsFromConvertedServicesIsCorrect() {
		Assert.assertEquals(0, carrierWShipmentsOnlyFromCarrierWServices.getServices().size());
		
		Assert.assertEquals(2, carrierWShipmentsOnlyFromCarrierWServices.getShipments().size());
		int demandShipments = 0;
		for (CarrierShipment carrierShipment : carrierWShipmentsOnlyFromCarrierWServices.getShipments()) {
			demandShipments += carrierShipment.getSize();
		}
		Assert.assertEquals(4, demandShipments);
	}
	
	@Test
	public void fleetAvailableAfterConvertingIsCorrect() {
		Assert.assertEquals(FleetSize.INFINITE, carrierWShipmentsOnlyFromCarrierWServices.getCarrierCapabilities().getFleetSize());
		Assert.assertEquals(1, carrierWShipmentsOnlyFromCarrierWServices.getCarrierCapabilities().getVehicleTypes().size());
		for (CarrierVehicleType carrierVehicleType : carrierWShipmentsOnlyFromCarrierWServices.getCarrierCapabilities().getVehicleTypes()){
			Assert.assertEquals(3,carrierVehicleType.getCarrierVehicleCapacity());
			Assert.assertEquals(130, carrierVehicleType.getVehicleCostInformation().fix, 0.0);
			Assert.assertEquals(0.0001, carrierVehicleType.getVehicleCostInformation().perDistanceUnit, 0.0);
			Assert.assertEquals(0.001, carrierVehicleType.getVehicleCostInformation().perTimeUnit, 0.0);
			Assert.assertEquals(10, carrierVehicleType.getMaximumVelocity(), 0.0);
			Assert.assertEquals(EngineInformation.FuelType.diesel, carrierVehicleType.getEngineInformation().getFuelType());
			Assert.assertEquals(0.015, carrierVehicleType.getEngineInformation().getGasConsumption(), 0.0);
		}
		
		Assert.assertEquals(FleetSize.INFINITE, carrierWShipmentsOnlyFromCarrierWShipments.getCarrierCapabilities().getFleetSize());
		Assert.assertEquals(1, carrierWShipmentsOnlyFromCarrierWShipments.getCarrierCapabilities().getVehicleTypes().size());
		for (CarrierVehicleType carrierVehicleType : carrierWShipmentsOnlyFromCarrierWShipments.getCarrierCapabilities().getVehicleTypes()){
			Assert.assertEquals(3,carrierVehicleType.getCarrierVehicleCapacity());
			Assert.assertEquals(130, carrierVehicleType.getVehicleCostInformation().fix, 0.0);
			Assert.assertEquals(0.0001, carrierVehicleType.getVehicleCostInformation().perDistanceUnit, 0.0);
			Assert.assertEquals(0.001, carrierVehicleType.getVehicleCostInformation().perTimeUnit, 0.0);
			Assert.assertEquals(10, carrierVehicleType.getMaximumVelocity(), 0.0);
			Assert.assertEquals(EngineInformation.FuelType.diesel, carrierVehicleType.getEngineInformation().getFuelType());
			Assert.assertEquals(0.015, carrierVehicleType.getEngineInformation().getGasConsumption(), 0.0);
		}
	}

	@Test
	public void copiingOfShipmentsIsDoneCorrectly() {
		Assert.assertEquals(2, carrierWShipmentsOnlyFromCarrierWShipments.getShipments().size());
		boolean foundShipment1 = false;
		boolean foundShipment2 = false;
		for (CarrierShipment carrierShipment :  carrierWShipmentsOnlyFromCarrierWShipments.getShipments()) {
			if (carrierShipment.getId() == Id.create("shipment1", CarrierShipment.class)) {
				System.out.println("Found Shipment1");
				foundShipment1 = true;
				Assert.assertEquals(Id.createLinkId("i(1,0)"), carrierShipment.getFrom());
				Assert.assertEquals(Id.createLinkId("i(7,6)R"), carrierShipment.getTo());
				Assert.assertEquals(1, carrierShipment.getSize());
				Assert.assertEquals(30.0, carrierShipment.getDeliveryServiceTime(), 0);
				Assert.assertEquals(3600.0, carrierShipment.getDeliveryTimeWindow().getStart(), 0);
				Assert.assertEquals(36000.0, carrierShipment.getDeliveryTimeWindow().getEnd(), 0);
				Assert.assertEquals(5.0, carrierShipment.getPickupServiceTime(), 0);
				Assert.assertEquals(0.0, carrierShipment.getPickupTimeWindow().getStart(), 0);
				Assert.assertEquals(7200.0, carrierShipment.getPickupTimeWindow().getEnd(), 0);
			} else if (carrierShipment.getId() == Id.create("shipment2", CarrierShipment.class)) {
				System.out.println("Found Shipment2");
				foundShipment2 = true;
				Assert.assertEquals(Id.createLinkId("(3,0)"), carrierShipment.getFrom());
				Assert.assertEquals(Id.createLinkId("i(3,7)"), carrierShipment.getTo());
				Assert.assertEquals(2, carrierShipment.getSize());
				Assert.assertEquals(30.0, carrierShipment.getDeliveryServiceTime(), 0);
				Assert.assertEquals(3600.0, carrierShipment.getDeliveryTimeWindow().getStart(), 0);
				Assert.assertEquals(36000.0, carrierShipment.getDeliveryTimeWindow().getEnd(), 0);
				Assert.assertEquals(5.0, carrierShipment.getPickupServiceTime(), 0);
				Assert.assertEquals(0.0, carrierShipment.getPickupTimeWindow().getStart(), 0);
				Assert.assertEquals(7200.0, carrierShipment.getPickupTimeWindow().getEnd(), 0);
			} 
			
//			Assert.assertTrue("Not found Shipment1 after copiing", foundShipment1);
			Assert.assertTrue("Not found Shipment2 after copiing", foundShipment2);
		}
	}
	
	
	@Test
	public void convertionOfServicesIsDoneCorrectly() {
		boolean foundSercice1 = false;
		boolean foundService2 = false;
		for (CarrierShipment carrierShipment :  carrierWShipmentsOnlyFromCarrierWServices.getShipments()) {
			if (carrierShipment.getId() == Id.create("Service1", CarrierShipment.class)) {
				foundSercice1 = true;
				Assert.assertEquals(Id.createLinkId("i(6,0)"), carrierShipment.getFrom());
				Assert.assertEquals(Id.createLinkId("i(3,9)"), carrierShipment.getTo());
				Assert.assertEquals(2, carrierShipment.getSize());
				Assert.assertEquals(31.0, carrierShipment.getDeliveryServiceTime(), 0);
				Assert.assertEquals(3601.0, carrierShipment.getDeliveryTimeWindow().getStart(), 0);
				Assert.assertEquals(36001.0, carrierShipment.getDeliveryTimeWindow().getEnd(), 0);
				Assert.assertEquals(0.0, carrierShipment.getPickupServiceTime(), 0);
				Assert.assertEquals(0.0, carrierShipment.getPickupTimeWindow().getStart(), 0);
				Assert.assertEquals(36001.0, carrierShipment.getPickupTimeWindow().getEnd(), 0);
			} else if (carrierShipment.getId() == Id.create("Service2", CarrierShipment.class)) {
				foundService2 = true;
				Assert.assertEquals(Id.createLinkId("i(6,0)"), carrierShipment.getFrom());
				Assert.assertEquals(Id.createLinkId("i(4,9)"), carrierShipment.getTo());
				Assert.assertEquals(2, carrierShipment.getSize());
				Assert.assertEquals(31.0, carrierShipment.getDeliveryServiceTime(), 0);
				Assert.assertEquals(3601.0, carrierShipment.getDeliveryTimeWindow().getStart(), 0);
				Assert.assertEquals(36001.0, carrierShipment.getDeliveryTimeWindow().getEnd(), 0);
				Assert.assertEquals(0.0, carrierShipment.getPickupServiceTime(), 0);
				Assert.assertEquals(0.0, carrierShipment.getPickupTimeWindow().getStart(), 0);
				Assert.assertEquals(36001.0, carrierShipment.getPickupTimeWindow().getEnd(), 0);
			}
			
			Assert.assertTrue("Not found converted Service1 after converting", foundSercice1);
			Assert.assertTrue("Not found converted Service2 after converting", foundService2);
		}
	}
	
	/* Note: This test can be removed / modified when jsprit works properly with a combined Service and Shipment VRP. 
	* Currently the capacity of the vehicle seems to be "ignored" in a way that the load within the tour is larger than the capacity;
	* Maybe it is because of the misunderstanding, that a Service is modeled as "Pickup" and not as thought before as "Delivery". KMT sep18
	*/
	@Test(expected=UnsupportedOperationException.class)
	public void exceptionIsThrownWhenUsingMixedShipmentsAndServices() {
		Carrier carrierMixedWServicesAndShipments = CarrierImpl.newInstance(Id.create("CarrierMixed", Carrier.class));
		carrierMixedWServicesAndShipments.getServices().add(createMatsimService("Service1", "i(3,9)", 2));
		carrierMixedWServicesAndShipments.getShipments().add(createMatsimShipment("shipment1", "i(1,0)", "i(7,6)R", 1)); 
		
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(getPackageInputDirectory() + "grid-network.xml"); 
		
		MatsimJspritFactory.createRoutingProblemBuilder(carrierMixedWServicesAndShipments, network);
	}
	
	private static CarrierShipment createMatsimShipment(String id, String from, String to, int size) {
		Id<CarrierShipment> shipmentId = Id.create(id, CarrierShipment.class);
		Id<Link> fromLinkId = null; 
		Id<Link> toLinkId= null;

		if(from != null ) {
			fromLinkId = Id.create(from, Link.class);
		} 
		if(to != null ) {
			toLinkId = Id.create(to, Link.class);
		}

		return CarrierShipment.Builder.newInstance(shipmentId, fromLinkId, toLinkId, size)
				.setDeliveryServiceTime(30.0)
				.setDeliveryTimeWindow(TimeWindow.newInstance(3600.0, 36000.0))
				.setPickupServiceTime(5.0)
				.setPickupTimeWindow(TimeWindow.newInstance(0.0, 7200.0))
				.build();
	}

	private static CarrierService createMatsimService(String id, String to, int size) {
		return CarrierService.Builder.newInstance(Id.create(id, CarrierService.class), Id.create(to, Link.class))
				.setCapacityDemand(size)
				.setServiceDuration(31.0)
				.setServiceStartTimeWindow(TimeWindow.newInstance(3601.0, 36001.0))
				.build();
	}
	
	private static String getPackageInputDirectory() {
		String classInputDirectory = "test/input/" + TestFreightUtils.class.getCanonicalName().replace('.', '/') + "/";
		String packageInputDirectory = classInputDirectory.substring(0, classInputDirectory.lastIndexOf('/'));
		packageInputDirectory = packageInputDirectory.substring(0, packageInputDirectory.lastIndexOf('/') + 1);
		return packageInputDirectory;
	}

}
