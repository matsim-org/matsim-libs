/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.utils;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.util.Solutions;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.carriers.jsprit.MatsimJspritFactory;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts;
import org.matsim.freight.carriers.jsprit.NetworkBasedTransportCosts.Builder;
import org.matsim.freight.carriers.jsprit.NetworkRouter;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;

//TODO: length of routes (legs) AND end time of route are missing.
/**
 * @author kturner
 *
 */
public class CarrierControllerUtilsIT{

	private final Id<Carrier> CARRIER_SERVICES_ID = Id.create("CarrierWServices", Carrier.class);
	private final Id<Carrier> CARRIER_SHIPMENTS_ID = Id.create("CarrierWShipments", Carrier.class);

	private Carrier carrierWServices;
	private Carrier carrierWShipments;

	private Carrier carrierWShipmentsOnlyFromCarrierWServices;
	private Carrier carrierWShipmentsOnlyFromCarrierWShipments;

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@BeforeEach
	public void setUp() {

		//Create carrier with services and shipments
		Carriers carriersWithServicesAndShipments = new Carriers();
		carrierWServices = CarriersUtils.createCarrier(CARRIER_SERVICES_ID );
		CarrierService service1 = createMatsimService("Service1", "i(3,9)", 2);
		CarriersUtils.addService(carrierWServices, service1);
		CarrierService service2 = createMatsimService("Service2", "i(4,9)", 2);
		CarriersUtils.addService(carrierWServices, service2);

		//Create carrier with shipments
		carrierWShipments = CarriersUtils.createCarrier(CARRIER_SHIPMENTS_ID );
		CarrierShipment shipment1 = createMatsimShipment("shipment1", "i(1,0)", "i(7,6)R", 1);
		CarriersUtils.addShipment(carrierWShipments, shipment1);
		CarrierShipment shipment2 = createMatsimShipment("shipment2", "i(3,0)", "i(3,7)", 2);
		CarriersUtils.addShipment(carrierWShipments, shipment2);

		//Create vehicle for Carriers
		final Id<VehicleType> vehTypeId = Id.create( "gridType", VehicleType.class );
		VehicleType carrierVehType = VehicleUtils.getFactory().createVehicleType( vehTypeId );
        EngineInformation engineInformation = carrierVehType.getEngineInformation() ;
		VehicleUtils.setHbefaTechnology(engineInformation, "diesel");
		VehicleUtils.setFuelConsumptionLitersPerMeter(engineInformation, 0.015);
		VehicleCapacity capacity = carrierVehType.getCapacity() ;
		capacity.setOther( 3. ) ;
		CostInformation costInfo = carrierVehType.getCostInformation();
		costInfo.setCostsPerSecond( 0.001 ) ;
		costInfo.setCostsPerMeter( 0.0001 ) ;
		costInfo.setFixedCost( 130. ) ;
//		VehicleType carrierVehType = CarriersUtils.CarrierVehicleTypeBuilder.newInstance( vehTypeId )
		carrierVehType.setMaximumVelocity(10);

		CarrierVehicleTypes vehicleTypes = new CarrierVehicleTypes() ;
		vehicleTypes.getVehicleTypes().put(carrierVehType.getId(), carrierVehType);

		CarrierVehicle carrierVehicle = CarrierVehicle.Builder.newInstance(Id.create("gridVehicle", org.matsim.vehicles.Vehicle.class), Id.createLinkId("i(6,0)"),
				carrierVehType ).setEarliestStart(0.0 ).setLatestEnd(36000.0 ).build();
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance()
				.addVehicle(carrierVehicle)
				.setFleetSize(FleetSize.INFINITE);
		carrierWServices.setCarrierCapabilities(ccBuilder.build());
		carrierWShipments.setCarrierCapabilities(ccBuilder.build());

		// Add both carriers
		carriersWithServicesAndShipments.addCarrier(carrierWServices);
		carriersWithServicesAndShipments.addCarrier(carrierWShipments);

		//load Network and build netbasedCosts for jsprit
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(testUtils.getPackageInputDirectory() + "grid-network.xml");
		Builder netBuilder = NetworkBasedTransportCosts.Builder.newInstance( network, vehicleTypes.getVehicleTypes().values() );
		final NetworkBasedTransportCosts netBasedCosts = netBuilder.build() ;
		netBuilder.setTimeSliceWidth(1800) ; // !!!!, otherwise it will not do anything.

		for (Carrier carrier : carriersWithServicesAndShipments.getCarriers().values()) {
			//Build VRP
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem problem = vrpBuilder.build();

				// get the algorithm out-of-the-box, search solution and get the best one.
			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

				//Routing bestPlan to Network
			CarrierPlan carrierPlanServicesAndShipments = MatsimJspritFactory.createPlan(carrier, bestSolution) ;
			NetworkRouter.routePlan(carrierPlanServicesAndShipments,netBasedCosts) ;
			carrier.addPlan(carrierPlanServicesAndShipments) ;
		}

		/*
		 * Now convert it to an only shipment-based VRP.
		 */

		//Convert to jsprit VRP
		Carriers carriersWithShipmentsOnly = CarriersUtils.createShipmentVRPCarrierFromServiceVRPSolution(
				carriersWithServicesAndShipments );

		for (Carrier carrier : carriersWithShipmentsOnly.getCarriers().values()) {
			//Build VRP
			VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
			vrpBuilder.setRoutingCost(netBasedCosts) ;
			VehicleRoutingProblem problem = vrpBuilder.build();

				// get the algorithm out-of-the-box, search solution and get the best one.
			VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
			Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
			VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

				//Routing bestPlan to Network
			CarrierPlan carrierPlanServicesAndShipments = MatsimJspritFactory.createPlan(carrier, bestSolution) ;
			NetworkRouter.routePlan(carrierPlanServicesAndShipments,netBasedCosts) ;
			carrier.addPlan(carrierPlanServicesAndShipments) ;
		}

		carrierWShipmentsOnlyFromCarrierWServices = carriersWithShipmentsOnly.getCarriers().get(CARRIER_SERVICES_ID);		//with converted Service
		carrierWShipmentsOnlyFromCarrierWShipments = carriersWithShipmentsOnly.getCarriers().get(CARRIER_SHIPMENTS_ID);		//with copied Shipments

	}


	@Test
	void numberOfToursIsCorrect() {
		Assertions.assertEquals(2, carrierWServices.getSelectedPlan().getScheduledTours().size());
		Assertions.assertEquals(1, carrierWShipments.getSelectedPlan().getScheduledTours().size());
		Assertions.assertEquals(1, carrierWShipmentsOnlyFromCarrierWServices.getSelectedPlan().getScheduledTours().size());
		Assertions.assertEquals(1, carrierWShipmentsOnlyFromCarrierWShipments.getSelectedPlan().getScheduledTours().size());
	}


	/**
	 * TODO Calculation of tour distance and duration are commented out, because ...tour.getEnd() has no values -> need for fixing in NetworkRouter or somewhere else kmt/okt18
	 */
	@Test
	void toursInitialCarrierWServicesIsCorrect() {
		Assertions.assertEquals(-270.462, carrierWServices.getSelectedPlan().getJspritScore(), MatsimTestUtils.EPSILON);	//Note: In score waiting and serviceDurationTime are not includes by now -> May fail, when fixed. KMT Okt/18
//		double tourDurationSum = 0;
//		for (ScheduledTour scheduledTour: carrierWServices.getSelectedPlan().getScheduledTours()){
//			tourDurationSum += scheduledTour.getTour().getEnd().getExpectedArrival() - scheduledTour.getDeparture();
//		}
//		Assert.assertEquals(9564.0 , tourDurationSum, 0);
//		double tourLengthSum = 0;
//		for (ScheduledTour scheduledTour: carrierWServices.getSelectedPlan().getScheduledTours()){
//			for (TourElement te : scheduledTour.getTour().getTourElements()) {
//				if (te instanceof Leg) {
//					tourLengthSum += ((Leg) te).getRoute().getDistance();
//				}
//			}
//		}
//		Assert.assertEquals(52000, tourLengthSum, 0);
	}

	/**
	 * TODO Calculation of tour distance and duration are commented out, because ...tour.getEnd() has no values -> need for fixing in NetworkRouter or somewhere else kmt/okt18
	 */
	@Test
	void toursInitialCarrierWShipmentsIsCorrect() {
		Assertions.assertEquals(-136.87, carrierWShipments.getSelectedPlan().getJspritScore(), MatsimTestUtils.EPSILON);			//Note: In score waiting and serviceDurationTime are not includes by now -> May fail, when fixed. KMT Okt/18

//		double tourDurationSum = 0;
//		for (ScheduledTour scheduledTour: carrierWShipments.getSelectedPlan().getScheduledTours()){
//			tourDurationSum += scheduledTour.getTour().getEnd().getExpectedArrival() - scheduledTour.getDeparture();
//		}
//		Assert.assertEquals(5260.0 , tourDurationSum, 0);
//
//		double tourLengthSum = 0;
//		for (ScheduledTour scheduledTour: carrierWShipments.getSelectedPlan().getScheduledTours()){
//			for (TourElement te : scheduledTour.getTour().getTourElements()) {
//				if (te instanceof Leg) {
//					tourLengthSum += ((Leg) te).getRoute().getDistance();
//				}
//			}
//		}
//		Assert.assertEquals(34000, tourLengthSum, 0);
	}

	/**
	 * TODO Calculation of tour distance and duration are commented out, because ...tour.getEnd() has no values -> need for fixing in NetworkRouter or somewhere else kmt/okt18
	 */
	@Test
	void toursCarrierWShipmentsOnlyFromCarrierWServicesIsCorrect() {
		Assertions.assertEquals(-140.462, carrierWShipmentsOnlyFromCarrierWServices.getSelectedPlan().getJspritScore(), MatsimTestUtils.EPSILON);	//Note: In score waiting and serviceDurationTime are not includes by now -> May fail, when fixed. KMT Okt/18

//		double tourDurationSum = 0;
//		for (ScheduledTour scheduledTour: carrierWShipmentsOnlyFromCarrierWServices.getSelectedPlan().getScheduledTours()){
//			tourDurationSum += scheduledTour.getTour().getEnd().getExpectedArrival() - scheduledTour.getDeparture();
//		}
//		Assert.assertEquals(7563.0 , tourDurationSum, 0);

//		double tourLengthSum = 0;
//		for (ScheduledTour scheduledTour: carrierWShipmentsOnlyFromCarrierWServices.getSelectedPlan().getScheduledTours()){
//			for (TourElement te : scheduledTour.getTour().getTourElements()) {
//				if (te instanceof Leg) {
////					System.out.println(((Leg) te).getRoute().getRouteDescription());
//					tourLengthSum += ((Leg) te).getRoute().getDistance();
////					System.out.println("Added:" + ((Leg) te).getRoute().getDistance() + " ; Sum Tour Length: " + tourLengthSum);
//				}
//			}
//		}
//		Assert.assertEquals(52000, tourLengthSum, 0);
	}

	/**
	 * TODO Calculation of tour distance and duration are commented out, because ...tour.getEnd() has no values -> need for fixing in NetworkRouter or somewhere else kmt/okt18
	 */
	@Test
	void toursCarrierWShipmentsOnlyFromCarrierWShipmentsIsCorrect() {
		Assertions.assertEquals(-136.87, carrierWShipmentsOnlyFromCarrierWShipments.getSelectedPlan().getJspritScore(), MatsimTestUtils.EPSILON);	//Note: In score waiting and serviceDurationTime are not includes by now -> May fail, when fixed. KMT Okt/18

//		double tourDurationSum = 0;
//		for (ScheduledTour scheduledTour: carrierWShipmentsOnlyFromCarrierWShipments.getSelectedPlan().getScheduledTours()){
//			tourDurationSum += scheduledTour.getTour().getEnd().getExpectedArrival() - scheduledTour.getDeparture();
//		}
//		Assert.assertEquals(5260.0 , tourDurationSum, 0);
//
//		double tourLengthSum = 0;
//		for (ScheduledTour scheduledTour: carrierWShipmentsOnlyFromCarrierWShipments.getSelectedPlan().getScheduledTours()){
//			for (TourElement te : scheduledTour.getTour().getTourElements()) {
//				if (te instanceof Leg) {
//					tourLengthSum += ((Leg) te).getRoute().getDistance();
//				}
//			}
//		}
//		Assert.assertEquals(34000, tourLengthSum, 0);
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
				.setDeliveryDuration(30.0)
				.setDeliveryStartingTimeWindow(TimeWindow.newInstance(0.0, 36000.0))
				.setPickupDuration(5.0)
				.setPickupStartingTimeWindow(TimeWindow.newInstance(0.0, 7200.0))
				.build();
	}

	private static CarrierService createMatsimService(String id, String to, int size) {
		CarrierService.Builder builder = CarrierService.Builder.newInstance(Id.create(id, CarrierService.class), Id.create(to, Link.class))
				.setCapacityDemand(size)
				.setServiceDuration(31.0);
		return builder.setServiceStartingTimeWindow(TimeWindow.newInstance(0.0, 36001.0))
				.build();
	}
}
