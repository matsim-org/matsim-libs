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

package org.matsim.freight.carriers.jsprit;

import static org.junit.jupiter.api.Assertions.*;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class MatsimTransformerTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void whenTransforming_jSpritType2matsimType_itIsMadeCorrectly() {
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType jspritType = VehicleTypeImpl.Builder
				.newInstance("myType").addCapacityDimension(0, 50).setCostPerDistance(10.0).setCostPerTransportTime(5.0)
				.setFixedCost(100.0).build();
		VehicleType matsimType = MatsimJspritFactory.createMatsimVehicleType(jspritType);
		assertNotNull(matsimType);
		assertEquals("myType", matsimType.getId().toString());
		assertEquals(50., matsimType.getCapacity().getWeightInTons(), Double.MIN_VALUE);
		assertEquals(10.0, matsimType.getCostInformation().getCostsPerMeter(), 0.01);
		assertEquals(5.0, matsimType.getCostInformation().getCostsPerSecond(), 0.01);
		assertEquals(100.0, matsimType.getCostInformation().getFixedCosts(), 0.01);
	}

	@Test
	void whenTransforming_jSpritType2matsimType_withCaching_itIsNotCached() {
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType jspritType = VehicleTypeImpl.Builder
				.newInstance("myType").addCapacityDimension(0, 50).setCostPerDistance(10.0).setCostPerTransportTime(5.0)
				.setFixedCost(100.0).build();
		VehicleType matsimType = MatsimJspritFactory.createMatsimVehicleType(jspritType);
		assertNotEquals(matsimType, MatsimJspritFactory.createMatsimVehicleType(jspritType));
	}

	@Test
	void whenTransforming_matsimType2jSpritType_itIsMadeCorrectly() {
		VehicleType matsimType = getMatsimVehicleType();
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType jspritType = MatsimJspritFactory
				.createJspritVehicleType(matsimType);
		assertNotNull(jspritType);
		assertEquals(50, jspritType.getCapacityDimensions().get(0));
		assertEquals(10.0, jspritType.getVehicleCostParams().perDistanceUnit, 0.01);
		assertEquals(5.0, jspritType.getVehicleCostParams().perTransportTimeUnit, 0.01);
		assertEquals(100.0, jspritType.getVehicleCostParams().fix, 0.01);
	}

	@Test
	void whenTransforming_jspritVehicle2matsimVehicle_itIsMadeCorrectly() {
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType jspritType = VehicleTypeImpl.Builder
				.newInstance("myType").addCapacityDimension(0, 50).setCostPerDistance(10.0).setCostPerTransportTime(5.0)
				.setFixedCost(100.0).build();
		Vehicle jspritVehicle = VehicleImpl.Builder.newInstance("myVehicle").setEarliestStart(10.0)
				.setLatestArrival(20.0).setStartLocation(Location.newInstance("loc")).setType(jspritType).build();
		CarrierVehicle matsimVehicle = MatsimJspritFactory.createCarrierVehicle(jspritVehicle);
		assertNotNull(matsimVehicle);
		assertEquals("myType", matsimVehicle.getType().getId().toString());
		assertEquals("myVehicle", matsimVehicle.getId().toString());
		assertEquals(10.0, matsimVehicle.getEarliestStartTime(), 0.01);
		assertEquals(20.0, matsimVehicle.getLatestEndTime(), 0.01);
		assertEquals("loc", matsimVehicle.getLinkId().toString() );
	}

	@Test
	void whenTransforming_matsimVehicle2jspritVehicle_itIsMadeCorrectly() {
		VehicleType matsimType = getMatsimVehicleType();
		CarrierVehicle matsimVehicle = getMatsimVehicle("matsimVehicle", "loc", matsimType);
		Vehicle jspritVehicle = MatsimJspritFactory.createJspritVehicle(matsimVehicle, null);
		assertNotNull(jspritVehicle);
		assertEquals("matsimType", jspritVehicle.getType().getTypeId());
		assertEquals("matsimVehicle", jspritVehicle.getId());
		assertEquals(10.0, jspritVehicle.getEarliestDeparture(), 0.01);
		assertEquals(20.0, jspritVehicle.getLatestArrival(), 0.01);
		assertEquals("loc", jspritVehicle.getStartLocation().getId());
	}

	@Test
	void whenTransforming_matsimService2jspritService_isMadeCorrectly() {
		CarrierService.Builder builder = CarrierService.Builder
				.newInstance(Id.create("serviceId", CarrierService.class), Id.create("locationId", Link.class))
				.setCapacityDemand(50).setServiceDuration(30.0);
		CarrierService carrierService = builder.setServiceStartingTimeWindow(TimeWindow.newInstance(10.0, 20.0)).build();
		Service service = MatsimJspritFactory.createJspritService(carrierService, null);
		assertNotNull(service);
		assertEquals("locationId", service.getLocation().getId());
		assertEquals(30.0, service.getServiceDuration(), 0.01);
		assertEquals(50, service.getSize().get(0));
		assertEquals(10.0, service.getTimeWindow().getStart(), 0.01);

		Service service2 = MatsimJspritFactory.createJspritService(carrierService, null);
		assertNotSame(service, service2);
		assertEquals(service, service2);
	}

	@Test
	void whenTransforming_jspritService2matsimService_isMadeCorrectly() {
		Service carrierService = Service.Builder.newInstance("serviceId").addSizeDimension(0, 50)
				.setLocation(Location.newInstance("locationId")).setServiceTime(30.0)
				.setTimeWindow(
						com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(10.0, 20.0))
				.build();

		CarrierService service = MatsimJspritFactory.createCarrierService(carrierService);
		assertNotNull(service);
		assertEquals("locationId", service.getServiceLinkId().toString());
		assertEquals(30.0, service.getServiceDuration(), 0.01);
		assertEquals(50, service.getCapacityDemand());
		assertEquals(10.0, service.getServiceStaringTimeWindow().getStart(), 0.01);

		CarrierService service2 = MatsimJspritFactory.createCarrierService(carrierService);
		assertNotSame(service, service2);
		assertEquals(service, service2);
	}

	@Test
	void whenTransforming_matsimShipment2jspritShipment_isMadeCorrectly() {
		CarrierShipment carrierShipment = CarrierShipment.Builder
				.newInstance(Id.create("ShipmentId", CarrierShipment.class), Id.createLinkId("PickupLocationId"),
						Id.createLinkId("DeliveryLocationId"), 50)
				.setPickupDuration(30.0).setPickupStartingTimeWindow(TimeWindow.newInstance(10.0, 20.0))
				.setDeliveryDuration(40.0).setDeliveryStartingTimeWindow(TimeWindow.newInstance(50.0, 60.0)).build();
		Shipment shipment = MatsimJspritFactory.createJspritShipment(carrierShipment);
		assertNotNull(shipment);
		assertEquals("PickupLocationId", shipment.getPickupLocation().getId());
		assertEquals(30.0, shipment.getPickupServiceTime(), 0.01);
		assertEquals(10.0, shipment.getPickupTimeWindow().getStart(), 0.01);
		assertEquals(20.0, shipment.getPickupTimeWindow().getEnd(), 0.01);
		assertEquals("DeliveryLocationId", shipment.getDeliveryLocation().getId());
		assertEquals(40.0, shipment.getDeliveryServiceTime(), 0.01);
		assertEquals(50.0, shipment.getDeliveryTimeWindow().getStart(), 0.01);
		assertEquals(60.0, shipment.getDeliveryTimeWindow().getEnd(), 0.01);
		assertEquals(50, shipment.getSize().get(0));

		Shipment shipment2 = MatsimJspritFactory.createJspritShipment(carrierShipment);
		assertNotSame(shipment, shipment2);
		assertEquals(shipment, shipment2);
	}

	@Test
	void whenTransforming_jspritShipment2matsimShipment_isMadeCorrectly() {
		Shipment shipment = Shipment.Builder.newInstance("shipmentId").addSizeDimension(0, 50)
				.setPickupLocation(Location.newInstance("PickupLocationId")).setPickupServiceTime(30.0)
				.setPickupTimeWindow(
						com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(10.0, 20.0))
				.setDeliveryLocation(Location.newInstance("DeliveryLocationId")).setDeliveryServiceTime(40.0)
				.setDeliveryTimeWindow(
						com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(50.0, 60.0))
				.build();

		CarrierShipment carrierShipment = MatsimJspritFactory.createCarrierShipment(shipment);
		assertNotNull(carrierShipment);
		assertEquals("PickupLocationId", carrierShipment.getPickupLinkId().toString());
		assertEquals(30.0, carrierShipment.getPickupDuration(), 0.01);
		assertEquals(10.0, carrierShipment.getPickupStartingTimeWindow().getStart(), 0.01);
		assertEquals(20.0, carrierShipment.getPickupStartingTimeWindow().getEnd(), 0.01);
		assertEquals("DeliveryLocationId", carrierShipment.getDeliveryLinkId().toString());
		assertEquals(40.0, carrierShipment.getDeliveryDuration(), 0.01);
		assertEquals(50.0, carrierShipment.getDeliveryStartingTimeWindow().getStart(), 0.01);
		assertEquals(60.0, carrierShipment.getDeliveryStartingTimeWindow().getEnd(), 0.01);
        assertEquals(50, carrierShipment.getCapacityDemand());

		CarrierShipment carrierShipment2 = MatsimJspritFactory.createCarrierShipment(shipment);
		assertNotSame(carrierShipment, carrierShipment2);
		assertEquals(carrierShipment, carrierShipment2);
	}

	@Test
	void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_routeStartMustBe15() {
		ScheduledTour sTour = getMatsimServiceTour();

		VehicleRoutingProblem vrp = getVehicleRoutingProblem(sTour);

		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vrp);
		assertEquals(15.0, route.getStart().getEndTime(), 0.01);
	}

	private VehicleRoutingProblem getVehicleRoutingProblem(ScheduledTour sTour) {
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		VehicleImpl vehicle = createJspritVehicle(sTour.getVehicle());
		vrpBuilder.addVehicle(vehicle);
		vrpBuilder.addAllJobs(getJobsFrom(sTour));
		return vrpBuilder.build();
	}

	private Collection<? extends Job> getJobsFrom(ScheduledTour sTour) {
		Collection<Service> services = new ArrayList<>();
		for (Tour.TourElement e : sTour.getTour().getTourElements()) {
			if (e instanceof Tour.TourActivity) {
				if (e instanceof Tour.ServiceActivity) {
					CarrierService carrierService = ((Tour.ServiceActivity) e)
							.getService();
					Service service = Service.Builder.newInstance(carrierService.getId().toString())
							.setLocation(Location.newInstance(carrierService.getServiceLinkId().toString())).build();
					services.add(service);
				}
			}
		}
		return services;
	}

	private VehicleImpl createJspritVehicle(CarrierVehicle vehicle) {
		return VehicleImpl.Builder.newInstance(vehicle.getId().toString())
					  .setEarliestStart(vehicle.getEarliestStartTime()).setLatestArrival(vehicle.getLatestEndTime())
					  .setStartLocation(Location.newInstance(vehicle.getLinkId().toString() ) ).build();
	}

	@Test
	void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_routeAndVehicleMustNotBeNull() {
		ScheduledTour sTour = getMatsimServiceTour();
		VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertNotNull(route);
		assertNotNull(route.getVehicle());
	}

	@Test
	void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_vehicleMustHaveTheCorrectId() {
		ScheduledTour sTour = getMatsimServiceTour();
		VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals("matsimVehicle", route.getVehicle().getId());
	}

	@Test
	void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_earliestStartMustBe10() {
		ScheduledTour sTour = getMatsimServiceTour();
		VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals(10.0, route.getStart().getTheoreticalEarliestOperationStartTime(), 0.01);
	}

	@Test
	void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_latestEndMustBe20() {
		ScheduledTour sTour = getMatsimServiceTour();
		VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals(20.0, route.getEnd().getTheoreticalLatestOperationStartTime(), 0.01);
	}

	@Test
	void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_sizeOfTourMustBe2() {
		ScheduledTour sTour = getMatsimServiceTour();
		VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals(2, route.getTourActivities().getActivities().size());
	}

	@Test
	void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_firstActIdMustBeCorrect() {
		ScheduledTour sTour = getMatsimServiceTour();
		VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals("to1", route.getTourActivities().getActivities().get(0).getLocation().getId());
	}

	@Test
	void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_secondActIdMustBeCorrect() {
		ScheduledTour sTour = getMatsimServiceTour();
		VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals("to2", route.getTourActivities().getActivities().get(1).getLocation().getId());
	}

	@Test
	void whenTransforming_matsimPlan2vehicleRouteSolution_itIsMadeCorrectly() {
		List<ScheduledTour> sTours = new ArrayList<>();
		ScheduledTour matsimTour = getMatsimTour("matsimVehicle");
		sTours.add(matsimTour);
		ScheduledTour matsimTour1 = getMatsimTour("matsimVehicle1");
		sTours.add(matsimTour1);

		VehicleImpl v1 = createJspritVehicle(matsimTour.getVehicle());
		VehicleImpl v2 = createJspritVehicle(matsimTour1.getVehicle());

		Collection<? extends Job> services1 = getJobsFrom(matsimTour);
		Collection<? extends Job> services2 = getJobsFrom(matsimTour1);

		VehicleRoutingProblem vehicleRoutingProblem = VehicleRoutingProblem.Builder.newInstance().addAllJobs(services1)
				.addAllJobs(services2).addVehicle(v1).addVehicle(v2).build();

		CarrierPlan plan = new CarrierPlan(CarriersUtils.createCarrier(Id.create("myCarrier", Carrier.class)), sTours);
		plan.setScore(-100.0);
		VehicleRoutingProblemSolution solution = MatsimJspritFactory.createSolution(plan, vehicleRoutingProblem);
		assertNotNull(solution);
		assertEquals(100.0, solution.getCost(), 0.01);
		assertEquals(2, solution.getRoutes().size());
	}

	private ScheduledTour getMatsimServiceTour() {
		CarrierService s1 = CarrierService.Builder
				.newInstance(Id.create("serviceId", CarrierService.class), Id.create("to1", Link.class))
				.setCapacityDemand(20).build();
		CarrierService s2 = CarrierService.Builder
				.newInstance(Id.create("serviceId2", CarrierService.class), Id.create("to2", Link.class))
				.setCapacityDemand(10).build();
		CarrierVehicle matsimVehicle = getMatsimVehicle("matsimVehicle", "loc", getMatsimVehicleType());
		double startTime = 15.0;
		Tour.Builder sTourBuilder = Tour.Builder.newInstance(Id.create("testTour", Tour.class));
		sTourBuilder.scheduleStart(matsimVehicle.getLinkId() );
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.scheduleService(s1);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.scheduleService(s2);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 60.0, 0.0));
		sTourBuilder.scheduleEnd(matsimVehicle.getLinkId() );
		return ScheduledTour.newInstance(sTourBuilder.build(), matsimVehicle, startTime);
	}

	private ScheduledTour getMatsimTour(String vehicleId) {
		CarrierShipment s1 = getMatsimShipment("s1", "from", "to1", 20);
		CarrierShipment s2 = getMatsimShipment("s2", "from", "to2", 20);
		CarrierVehicle matsimVehicle = getMatsimVehicle(vehicleId, "loc", getMatsimVehicleType());
		double startTime = 15.0;
		Tour.Builder sTourBuilder = Tour.Builder.newInstance(Id.create("testTour", Tour.class));
		sTourBuilder.scheduleStart(matsimVehicle.getLinkId() );
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.schedulePickup(s1);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.schedulePickup(s2);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.scheduleDelivery(s1);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 50.0, 0.0));
		sTourBuilder.scheduleDelivery(s2);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 60.0, 0.0));
		sTourBuilder.scheduleEnd(matsimVehicle.getLinkId() );
		return ScheduledTour.newInstance(sTourBuilder.build(), matsimVehicle, startTime);
	}

	private CarrierVehicle getMatsimVehicle(String VehicleId, String locationId, VehicleType matsimType) {
		return CarrierVehicle.Builder
				.newInstance(Id.create(VehicleId, org.matsim.vehicles.Vehicle.class), Id.create(locationId, Link.class), matsimType )
				.setEarliestStart(10.0).setLatestEnd(20.0).build();
	}

	private VehicleType getMatsimVehicleType() {
		VehicleType vehicleType = VehicleUtils.getFactory()
				.createVehicleType(Id.create("matsimType", VehicleType.class)).setMaximumVelocity(13.8);
		vehicleType.getCapacity().setOther(50);
		vehicleType.getCostInformation().setCostsPerMeter(10.0).setCostsPerSecond(5.0).setFixedCost(100.);
		VehicleUtils.setHbefaTechnology(vehicleType.getEngineInformation(), "diesel");
		VehicleUtils.setFuelConsumptionLitersPerMeter(vehicleType.getEngineInformation(), 0.015);

		return vehicleType;
	}

	private CarrierShipment getMatsimShipment(String id, String from, String to, int size) {
		return CarrierShipment.Builder
				.newInstance(Id.create(id, CarrierShipment.class), Id.create(from, Link.class),
						Id.create(to, Link.class), size)
				.setDeliveryDuration(30.0).setDeliveryStartingTimeWindow(TimeWindow.newInstance(10.0, 20.0))
				.setPickupDuration(15.0).setPickupStartingTimeWindow(TimeWindow.newInstance(1.0, 5.0)).build();
	}

	@Test
	void createVehicleRoutingProblemBuilderWithServices_isMadeCorrectly() {
		Carrier carrier = createCarrierWithServices();
        Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(testUtils.getClassInputDirectory() + "grid-network.xml");
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
		VehicleRoutingProblem vrp = vrpBuilder.build();

		// check vehicle (type) data
		Vehicle vehicle = vrp.getVehicles().iterator().next();
		assertNotNull(vrp);
		assertEquals(com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize.INFINITE, vrp.getFleetSize());
		assertEquals(1, vrp.getVehicles().size());
		assertEquals("matsimVehicle", vehicle.getId());
		assertEquals("i(6,0)", vehicle.getStartLocation().getId());
		assertEquals(10.0, vehicle.getEarliestDeparture(), 0.0);
		assertEquals(20.0, vehicle.getLatestArrival(), 0.0);
		assertEquals("matsimType", vehicle.getType().getTypeId());
		assertEquals(10.0, vehicle.getType().getVehicleCostParams().perDistanceUnit, 0.0);
		assertEquals(5.0, vehicle.getType().getVehicleCostParams().perTransportTimeUnit, 0.0);
		assertEquals(100.0, vehicle.getType().getVehicleCostParams().fix, 0.0);
		assertEquals("diesel", VehicleUtils.getHbefaTechnology(((VehicleType)vehicle.getType().getUserData()).getEngineInformation()));
		assertEquals(0.015, VehicleUtils.getFuelConsumptionLitersPerMeter(((VehicleType)vehicle.getType().getUserData()).getEngineInformation()));
		assertEquals(13.8, vehicle.getType().getMaxVelocity(), 0.0);

		// check service data
		Job jobS1 = vrp.getJobs().get("serviceId");
		assertNotNull(jobS1);
		assertEquals("serviceId", jobS1.getId());
		assertEquals(20, jobS1.getSize().get(0));
		assertInstanceOf(Service.class, jobS1);
		Service service1 = (Service) jobS1;
		assertEquals(20, service1.getSize().get(0));
		assertEquals(10.0, service1.getServiceDuration(), 0.0);
		assertEquals("i(7,4)R", service1.getLocation().getId());

		Job jobS2 = vrp.getJobs().get("serviceId2");
		assertNotNull(jobS2);
		assertEquals("serviceId2", jobS2.getId());
		assertEquals(10, jobS2.getSize().get(0));
		assertInstanceOf(Service.class, jobS2);
		Service service2 = (Service) jobS2;
		assertEquals(10, service2.getSize().get(0));
		assertEquals(20.0, service2.getServiceDuration(), 0.0);
		assertEquals("i(3,9)", service2.getLocation().getId());
	}

	//	@Disabled		//Set to ignore due to not implemented functionality of Shipments in MatsimJspritFactory
	@Test
	void createVehicleRoutingProblemBuilderWithShipments_isMadeCorrectly() {
        Carrier carrier = createCarrierWithShipments();
        Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(testUtils.getClassInputDirectory() + "grid-network.xml");
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);
		VehicleRoutingProblem vrp = vrpBuilder.build();

		// check vehicle (type) data
		Vehicle vehicle = vrp.getVehicles().iterator().next();
		assertNotNull(vrp);
		assertEquals(com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize.INFINITE, vrp.getFleetSize());
		assertEquals(1, vrp.getVehicles().size());
		assertEquals("matsimVehicle", vehicle.getId());
		assertEquals("i(6,0)", vehicle.getStartLocation().getId());
		assertEquals(10.0, vehicle.getEarliestDeparture(), 0.0);
		assertEquals(20.0, vehicle.getLatestArrival(), 0.0);
		assertEquals("matsimType", vehicle.getType().getTypeId());
		assertEquals(10.0, vehicle.getType().getVehicleCostParams().perDistanceUnit, 0.0);
		assertEquals(5.0, vehicle.getType().getVehicleCostParams().perTransportTimeUnit, 0.0);
		assertEquals(100.0, vehicle.getType().getVehicleCostParams().fix, 0.0);
		// assertEquals(FuelType.diesel, vehicle. ...); //TODO
		// assertEquals(15, FuelConsumption ...); //TODO
		assertEquals(13.8, vehicle.getType().getMaxVelocity(), 0.0);

		// check service data
		Job jobS1 = vrp.getJobs().get("shipment1");
		assertNotNull(jobS1);
		assertEquals("shipment1", jobS1.getId());
		assertEquals(10, jobS1.getSize().get(0));
		assertInstanceOf(Shipment.class, jobS1);
		Shipment shipment1 = (Shipment) jobS1;
		assertEquals(10, shipment1.getSize().get(0));
		assertEquals("i(6,0)", shipment1.getPickupLocation().getId());
		assertEquals(15.0, shipment1.getPickupServiceTime(), 0.0);
		assertEquals(1.0, shipment1.getPickupTimeWindow().getStart(), 0.0);
		assertEquals(5.0, shipment1.getPickupTimeWindow().getEnd(), 0.0);
		assertEquals("i(7,4)R", shipment1.getDeliveryLocation().getId());
		assertEquals(30.0, shipment1.getDeliveryServiceTime(), 0.0);
		assertEquals(10.0, shipment1.getDeliveryTimeWindow().getStart(), 0.0);
		assertEquals(20.0, shipment1.getDeliveryTimeWindow().getEnd(), 0.0);

		Job jobS2 = vrp.getJobs().get("shipment2");
		assertNotNull(jobS2);
		assertEquals("shipment2", jobS2.getId());
		assertEquals(20, jobS2.getSize().get(0));
		assertInstanceOf(Shipment.class, jobS2);
		Shipment shipment2 = (Shipment) jobS2;
		assertEquals(20, shipment2.getSize().get(0));
		assertEquals("i(3,9)", shipment2.getDeliveryLocation().getId());
	}

	private Carrier createCarrierWithServices() {
		Carrier carrier = CarriersUtils.createCarrier(Id.create("TestCarrier", Carrier.class));
		VehicleType matsimType = getMatsimVehicleType();
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance()
				.addVehicle(getMatsimVehicle("matsimVehicle", "i(6,0)", matsimType)).setFleetSize(FleetSize.INFINITE);
		carrier.setCarrierCapabilities(ccBuilder.build());
		CarrierService carrierService1 = CarrierService.Builder
				.newInstance(Id.create("serviceId", CarrierService.class), Id.create("i(7,4)R", Link.class))
				.setCapacityDemand(20).setServiceDuration(10.0).build();
		CarriersUtils.addService(carrier, carrierService1);
		CarrierService carrierService2 = CarrierService.Builder
				.newInstance(Id.create("serviceId2", CarrierService.class), Id.create("i(3,9)", Link.class))
				.setCapacityDemand(10).setServiceDuration(20.0).build();
		CarriersUtils.addService(carrier, carrierService2);
		return carrier;
	}

	private Carrier createCarrierWithShipments() {
		Carrier carrier = CarriersUtils.createCarrier(Id.create("TestCarrier", Carrier.class));
		VehicleType matsimType = getMatsimVehicleType();
		CarrierCapabilities.Builder ccBuilder = CarrierCapabilities.Builder.newInstance()
				.addVehicle(getMatsimVehicle("matsimVehicle", "i(6,0)", matsimType)).setFleetSize(FleetSize.INFINITE);
		carrier.setCarrierCapabilities(ccBuilder.build());
		CarrierShipment shipment1 = getMatsimShipment("shipment1", "i(6,0)", "i(7,4)R", 10);
		CarriersUtils.addShipment(carrier, shipment1);
		CarrierShipment shipment2 = getMatsimShipment("shipment2", "i(6,0)", "i(3,9)", 20);
		CarriersUtils.addShipment(carrier, shipment2);
		return carrier;
	}

}
