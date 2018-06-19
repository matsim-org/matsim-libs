package org.matsim.contrib.freight.jsprit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.Tour;

public class TestMatsimTransformer {
	
	
	@Test
	public void whenTransforming_jSpritType2matsimType_itIsMadeCorrectly(){
		VehicleType jspritType = VehicleTypeImpl.Builder.newInstance("myType").addCapacityDimension(0, 50).setCostPerDistance(10.0).setCostPerTransportTime(5.0).setFixedCost(100.0).build();
		CarrierVehicleType matsimType = MatsimJspritFactory.createCarrierVehicleType(jspritType);
		assertNotNull(matsimType);
		assertEquals("myType",matsimType.getId().toString());
		assertEquals(50, matsimType.getCarrierVehicleCapacity());
		assertEquals(10.0, matsimType.getVehicleCostInformation().perDistanceUnit, 0.01);
		assertEquals(5.0, matsimType.getVehicleCostInformation().perTimeUnit, 0.01);
		assertEquals(100.0, matsimType.getVehicleCostInformation().fix, 0.01);
	}
	
	@Test
	public void whenTransforming_jSpritType2matsimType_withCaching_itIsNotCached(){
		VehicleType jspritType = VehicleTypeImpl.Builder.newInstance("myType").addCapacityDimension(0, 50).setCostPerDistance(10.0).setCostPerTransportTime(5.0).setFixedCost(100.0).build();
		CarrierVehicleType matsimType = MatsimJspritFactory.createCarrierVehicleType(jspritType);
		assertThat(matsimType, is(not(MatsimJspritFactory.createCarrierVehicleType(jspritType))));
	}
	
	
	@Test
	public void whenTransforming_matsimType2jSpritType_itIsMadeCorrectly(){
		CarrierVehicleType matsimType = getMatsimVehicleType();
		VehicleType jspritType = MatsimJspritFactory.createVehicleType(matsimType);
		assertNotNull(jspritType);
		assertEquals(50,jspritType.getCapacityDimensions().get(0));
		assertEquals(10.0, jspritType.getVehicleCostParams().perDistanceUnit,0.01);
		assertEquals(5.0, jspritType.getVehicleCostParams().perTransportTimeUnit,0.01);
		assertEquals(100.0, jspritType.getVehicleCostParams().fix,0.01);
	}
	
	@Test
	public void whenTransforming_jspritVehicle2matsimVehicle_itIsMadeCorrectly(){
		VehicleType jspritType = VehicleTypeImpl.Builder.newInstance("myType").addCapacityDimension(0, 50).setCostPerDistance(10.0).setCostPerTransportTime(5.0).setFixedCost(100.0).build();
		Vehicle jspritVehicle = VehicleImpl.Builder.newInstance("myVehicle").setEarliestStart(10.0).setLatestArrival(20.0).setStartLocation(Location.newInstance("loc")).setType(jspritType).build();
		CarrierVehicle matsimVehicle = MatsimJspritFactory.createCarrierVehicle(jspritVehicle);
		assertNotNull(matsimVehicle);
		assertEquals("myType", matsimVehicle.getVehicleType().getId().toString());
		assertEquals("myVehicle",matsimVehicle.getVehicleId().toString());
		assertEquals(10.0,matsimVehicle.getEarliestStartTime(),0.01);
		assertEquals(20.0,matsimVehicle.getLatestEndTime(),0.01);
		assertEquals("loc",matsimVehicle.getLocation().toString());
	}
	
	@Test
	public void whenTransforming_matsimVehicle2jspritVehicle_itIsMadeCorrectly(){
		CarrierVehicleType matsimType = getMatsimVehicleType();
		CarrierVehicle matsimVehicle = getMatsimVehicle(matsimType, "matsimVehicle");
		Vehicle jspritVehicle = MatsimJspritFactory.createVehicle(matsimVehicle, null);
		assertNotNull(jspritVehicle);
		assertEquals("matsimType", jspritVehicle.getType().getTypeId());
		assertEquals("matsimVehicle",jspritVehicle.getId());
		assertEquals(10.0,jspritVehicle.getEarliestDeparture(),0.01);
		assertEquals(20.0,jspritVehicle.getLatestArrival(),0.01);
		assertEquals("loc",jspritVehicle.getStartLocation().getId());
	}
	
	@Test
	public void whenTransforming_matsimService2jspritService_isMadeCorrectly(){
		CarrierService carrierService = CarrierService.Builder.newInstance(Id.create("serviceId", CarrierService.class), Id.create("locationId", Link.class))
				.setCapacityDemand(50).setServiceDuration(30.0).setServiceStartTimeWindow(TimeWindow.newInstance(10.0, 20.0)).build();
		Service service = MatsimJspritFactory.createService(carrierService, null);
		assertNotNull(service);
		assertEquals("locationId", service.getLocation().getId());
		assertEquals(30.0 , service.getServiceDuration(), 0.01);
		assertEquals(50, service.getSize().get(0));
		assertEquals(10.0, service.getTimeWindow().getStart(),0.01);
		
		Service service2 = MatsimJspritFactory.createService(carrierService, null);
		assertTrue(service != service2);
		assertTrue(service.equals(service2));
	}
	
	@Test
	public void whenTransforming_jspritService2matsimService_isMadeCorrectly(){
		Service carrierService = Service.Builder.newInstance("serviceId").addSizeDimension(0, 50).setLocation(Location.newInstance("locationId"))
				.setServiceTime(30.0).setTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(10.0, 20.0)).build();
		
		CarrierService service = MatsimJspritFactory.createCarrierService(carrierService);
		assertNotNull(service);
		assertEquals("locationId", service.getLocationLinkId().toString());
		assertEquals(30.0 , service.getServiceDuration(), 0.01);
		assertEquals(50, service.getCapacityDemand());
		assertEquals(10.0, service.getServiceStartTimeWindow().getStart(),0.01);
		
		CarrierService service2 = MatsimJspritFactory.createCarrierService(carrierService);
		assertTrue(service != service2);
		assertTrue(service.equals(service2));
	}
	
	@Test
	public void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_routeStartMustBe15(){
		ScheduledTour sTour = getMatsimServiceTour();

        VehicleRoutingProblem vrp = getVehicleRoutingProblem(sTour);

        VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vrp);
		assertEquals(15.0, route.getStart().getEndTime(),0.01);
	}

    private VehicleRoutingProblem getVehicleRoutingProblem(ScheduledTour sTour) {
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        VehicleImpl vehicle = createJspritVehicle(sTour.getVehicle());
        vrpBuilder.addVehicle(vehicle);
        vrpBuilder.addAllJobs(getJobsFrom(sTour));
        return vrpBuilder.build();
    }

    private Collection<? extends Job> getJobsFrom(ScheduledTour sTour) {
        Collection<Service> services = new ArrayList<Service>();
        for(Tour.TourElement e : sTour.getTour().getTourElements()){
            if(e instanceof org.matsim.contrib.freight.carrier.Tour.TourActivity){
                if(e instanceof org.matsim.contrib.freight.carrier.Tour.ServiceActivity){
                    CarrierService carrierService = ((org.matsim.contrib.freight.carrier.Tour.ServiceActivity) e).getService();
                    Service service = Service.Builder.newInstance(carrierService.getId().toString()).setLocation(Location.newInstance(carrierService.getLocationLinkId().toString())).build();
                    services.add(service);
                }
            }
        }
        return services;
    }

    private VehicleImpl createJspritVehicle(CarrierVehicle vehicle) {
        return VehicleImpl.Builder.newInstance(vehicle.getVehicleId().toString())
                .setEarliestStart(vehicle.getEarliestStartTime())
                .setLatestArrival(vehicle.getLatestEndTime())
                .setStartLocation(Location.newInstance(vehicle.getLocation().toString())).build();
    }

    @Test
	public void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_routeAndVehicleMustNotBeNull(){
		ScheduledTour sTour = getMatsimServiceTour();
        VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertNotNull(route);
		assertNotNull(route.getVehicle());
	}

	@Test
	public void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_vehicleMustHaveTheCorrectId(){
		ScheduledTour sTour = getMatsimServiceTour();
        VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals("matsimVehicle", route.getVehicle().getId());
	}

	@Test
	public void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_earliestStartMustBe10(){
		ScheduledTour sTour = getMatsimServiceTour();
        VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals(10.0, route.getStart().getTheoreticalEarliestOperationStartTime(),0.01);
	}

	@Test
	public void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_latestEndMustBe20(){
		ScheduledTour sTour = getMatsimServiceTour();
        VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals(20.0, route.getEnd().getTheoreticalLatestOperationStartTime(),0.01);
	}

	@Test
	public void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_sizeOfTourMustBe2(){
		ScheduledTour sTour = getMatsimServiceTour();
        VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals(2,route.getTourActivities().getActivities().size());
	}

	@Test
	public void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_firstActIdMustBeCorrect(){
		ScheduledTour sTour = getMatsimServiceTour();
        VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals("to1",route.getTourActivities().getActivities().get(0).getLocation().getId());
	}

	@Test
	public void whenTransforming_matsimScheduledTourWithServiceAct2vehicleRoute_secondActIdMustBeCorrect(){
		ScheduledTour sTour = getMatsimServiceTour();
        VehicleRoutingProblem vehicleRoutingProblem = getVehicleRoutingProblem(sTour);
		VehicleRoute route = MatsimJspritFactory.createRoute(sTour, vehicleRoutingProblem);
		assertEquals("to2",route.getTourActivities().getActivities().get(1).getLocation().getId());
	}

	@Test
	public void whenTransforming_matsimPlan2vehicleRouteSolution_itIsMadeCorrectly(){
		List<ScheduledTour> sTours = new ArrayList<ScheduledTour>();
        ScheduledTour matsimTour = getMatsimTour("matsimVehicle");
        sTours.add(matsimTour);
        ScheduledTour matsimTour1 = getMatsimTour("matsimVehicle1");
        sTours.add(matsimTour1);

        VehicleImpl v1 = createJspritVehicle(matsimTour.getVehicle());
        VehicleImpl v2 = createJspritVehicle(matsimTour1.getVehicle());

        Collection<? extends Job> services1 = getJobsFrom(matsimTour);
        Collection<? extends Job> services2 = getJobsFrom(matsimTour1);

        VehicleRoutingProblem vehicleRoutingProblem = VehicleRoutingProblem.Builder.newInstance().addAllJobs(services1).addAllJobs(services2).addVehicle(v1).addVehicle(v2).build();

		CarrierPlan plan = new CarrierPlan(CarrierImpl.newInstance(Id.create("myCarrier", Carrier.class)), sTours);
		plan.setScore(-100.0);
		VehicleRoutingProblemSolution solution = MatsimJspritFactory.createSolution(plan, vehicleRoutingProblem);
		assertNotNull(solution);
		assertEquals(100.0, solution.getCost(), 0.01);
		assertEquals(2,solution.getRoutes().size());
	}

	private ScheduledTour getMatsimServiceTour() {
		CarrierService s1 = CarrierService.Builder.newInstance(Id.create("serviceId", CarrierService.class),Id.create("to1", Link.class)).setCapacityDemand(20).build();
		CarrierService s2 = CarrierService.Builder.newInstance(Id.create("serviceId2", CarrierService.class),Id.create("to2", Link.class)).setCapacityDemand(10).build();
		CarrierVehicle matsimVehicle = getMatsimVehicle(getMatsimVehicleType(), "matsimVehicle");
		double startTime = 15.0;
		Tour.Builder sTourBuilder = Tour.Builder.newInstance();
		sTourBuilder.scheduleStart(matsimVehicle.getLocation());
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.scheduleService(s1);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.scheduleService(s2);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 60.0, 0.0));
		sTourBuilder.scheduleEnd(matsimVehicle.getLocation());
		return ScheduledTour.newInstance(sTourBuilder.build(),matsimVehicle,startTime);
	}
	
	private ScheduledTour getMatsimTour(String vehicleId) {
		CarrierShipment s1 = getMatsimShipment("from", "to1", 20);
		CarrierShipment s2 = getMatsimShipment("from", "to2", 20);
		CarrierVehicle matsimVehicle = getMatsimVehicle(getMatsimVehicleType(), vehicleId);
		double startTime = 15.0;
		Tour.Builder sTourBuilder = Tour.Builder.newInstance();
		sTourBuilder.scheduleStart(matsimVehicle.getLocation());
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.schedulePickup(s1);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.schedulePickup(s2);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 15.0, 0.0));
		sTourBuilder.scheduleDelivery(s1);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 50.0, 0.0));
		sTourBuilder.scheduleDelivery(s2);
		sTourBuilder.addLeg(sTourBuilder.createLeg(null, 60.0, 0.0));
		sTourBuilder.scheduleEnd(matsimVehicle.getLocation());
		return ScheduledTour.newInstance(sTourBuilder.build(),matsimVehicle,startTime);
	}

	private CarrierVehicle getMatsimVehicle(CarrierVehicleType matsimType, String VehicleId) {
		return CarrierVehicle.Builder.newInstance(Id.create(VehicleId, org.matsim.vehicles.Vehicle.class), Id.create("loc", Link.class)).setEarliestStart(10.0).setLatestEnd(20.0).setType(matsimType).build();
	}

	private CarrierVehicleType getMatsimVehicleType() {
		CarrierVehicleType matsimType = CarrierVehicleType.Builder.newInstance(Id.create("matsimType", org.matsim.vehicles.VehicleType.class)).setCapacity(50).setCostPerDistanceUnit(10.0).setCostPerTimeUnit(5.0)
				.setFixCost(100.0).build();
		return matsimType;
	}

	private CarrierShipment getMatsimShipment(String from, String to, int size) {
		return CarrierShipment.Builder.newInstance(Id.create(from, Link.class), Id.create(to, Link.class), size).setDeliveryServiceTime(30.0).
				setDeliveryTimeWindow(TimeWindow.newInstance(10.0, 20.0)).build();
	}
}
