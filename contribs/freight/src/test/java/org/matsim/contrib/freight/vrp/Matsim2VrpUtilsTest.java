package org.matsim.contrib.freight.vrp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanReader;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.vrp.basics.Delivery;
import org.matsim.contrib.freight.vrp.basics.End;
import org.matsim.contrib.freight.vrp.basics.Pickup;
import org.matsim.contrib.freight.vrp.basics.Start;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

public class Matsim2VrpUtilsTest extends MatsimTestCase{
	
	Carriers carriers;
	
	public void setUp() throws Exception{
		super.setUp();
		carriers = new Carriers();
		new CarrierPlanReader(carriers).read(getInputDirectory() + "carrierPlansEquils.xml");
	}
	
	public void testCreateVehicleRoutes(){
		Carrier carrier = carriers.getCarriers().get(new IdImpl("carrier1"));
		Matsim2VrpMap vrpMap = new Matsim2VrpMap(carrier.getShipments(), carrier.getCarrierCapabilities().getCarrierVehicles());
		Collection<VehicleRoute> routes = Matsim2VrpUtils.createVehicleRoutes(carrier.getSelectedPlan().getScheduledTours(), vrpMap);
		List<VehicleRoute> vRoutes = new ArrayList<VehicleRoute>(routes);
		assertEquals(2, routes.size());
		VehicleRoute route1 = vRoutes.get(0);
		assertEquals("vehicle_c1",route1.getVehicle().getId());
		assertEquals(4,route1.getTour().getActivities().size());
		assertTrue(route1.getTour().getActivities().get(0) instanceof Start);
		assertTrue(route1.getTour().getActivities().get(1) instanceof Pickup);
		assertTrue(route1.getTour().getActivities().get(2) instanceof Delivery);
		assertTrue(route1.getTour().getActivities().get(3) instanceof End);
		assertEquals("1", route1.getTour().getActivities().get(0).getLocationId());
		assertEquals("15", route1.getTour().getActivities().get(1).getLocationId());
		assertEquals("22", route1.getTour().getActivities().get(2).getLocationId());
		assertEquals("1", route1.getTour().getActivities().get(3).getLocationId());
		assertEquals("1", route1.getTour().getActivities().get(0).getLocationId());
		assertEquals(7*3600.0,route1.getTour().getActivities().get(0).getEarliestOperationStartTime());
		assertEquals(7*3600.0,route1.getTour().getActivities().get(0).getLatestOperationStartTime());
		assertEquals(21660.0,route1.getTour().getActivities().get(1).getEarliestOperationStartTime());
		assertEquals(6*3600.0,route1.getTour().getActivities().get(3).getEarliestOperationStartTime());
		assertEquals(86399.0,route1.getTour().getActivities().get(3).getLatestOperationStartTime());
		
		
		VehicleRoute route2 = vRoutes.get(1);
		assertEquals("vehicle_c2",route2.getVehicle().getId());
		assertEquals(4,route2.getTour().getActivities().size());
		assertTrue(route2.getTour().getActivities().get(0) instanceof Start);
		assertTrue(route2.getTour().getActivities().get(1) instanceof Pickup);
		assertTrue(route2.getTour().getActivities().get(2) instanceof Delivery);
		assertTrue(route2.getTour().getActivities().get(3) instanceof End);
		assertEquals("1", route2.getTour().getActivities().get(0).getLocationId());
		assertEquals("15", route2.getTour().getActivities().get(1).getLocationId());
		assertEquals("23", route2.getTour().getActivities().get(2).getLocationId());
		assertEquals("1", route2.getTour().getActivities().get(3).getLocationId());
		assertEquals("1", route2.getTour().getActivities().get(0).getLocationId());
		assertEquals(7*3600.0,route2.getTour().getActivities().get(0).getEarliestOperationStartTime());
		assertEquals(7*3600.0,route2.getTour().getActivities().get(0).getLatestOperationStartTime());
		assertEquals(21660.0,route2.getTour().getActivities().get(1).getEarliestOperationStartTime());
		assertEquals(6*3600.0,route2.getTour().getActivities().get(3).getEarliestOperationStartTime());
		assertEquals(86399.0,route2.getTour().getActivities().get(3).getLatestOperationStartTime());
	}
	
	public void testCreateTours(){
		Carrier carrier = carriers.getCarriers().get(new IdImpl("carrier1"));
		Matsim2VrpMap vrpMap = new Matsim2VrpMap(carrier.getShipments(), carrier.getCarrierCapabilities().getCarrierVehicles());
		Collection<VehicleRoute> routes = Matsim2VrpUtils.createVehicleRoutes(carrier.getSelectedPlan().getScheduledTours(), vrpMap);
		List<VehicleRoute> vRoutes = new ArrayList<VehicleRoute>(routes);
		
		Collection<ScheduledTour> sTours = Matsim2VrpUtils.createTours(routes, vrpMap);
		assertEquals(carrier.getSelectedPlan().getScheduledTours().size(), sTours.size());
		
		List<ScheduledTour> tourList = new ArrayList<ScheduledTour>(sTours);
		assertEquals(2, tourList.size());
		ScheduledTour route1 = tourList.get(0);
		assertEquals("vehicle_c1",route1.getVehicle().getVehicleId().toString());
		assertEquals(5,route1.getTour().getTourElements().size());
		assertTrue(route1.getTour().getTourElements().get(0) instanceof Leg);
		assertTrue(route1.getTour().getTourElements().get(1) instanceof org.matsim.contrib.freight.carrier.Tour.Pickup);
		assertTrue(route1.getTour().getTourElements().get(2) instanceof Leg);
		assertTrue(route1.getTour().getTourElements().get(3) instanceof org.matsim.contrib.freight.carrier.Tour.Delivery);
		assertEquals("1", route1.getTour().getStartLinkId().toString());
		assertEquals("15", ((org.matsim.contrib.freight.carrier.Tour.Pickup)route1.getTour().getTourElements().get(1)).getLocation().toString());
		assertEquals("22", ((org.matsim.contrib.freight.carrier.Tour.Delivery)route1.getTour().getTourElements().get(3)).getLocation().toString());
		assertEquals("1", route1.getTour().getEndLinkId().toString());
		
		ScheduledTour route2 = tourList.get(1);
		assertEquals("vehicle_c2",route2.getVehicle().getVehicleId().toString());
		assertEquals(5,route1.getTour().getTourElements().size());
		assertTrue(route2.getTour().getTourElements().get(0) instanceof Leg);
		assertTrue(route2.getTour().getTourElements().get(1) instanceof org.matsim.contrib.freight.carrier.Tour.Pickup);
		assertTrue(route2.getTour().getTourElements().get(2) instanceof Leg);
		assertTrue(route2.getTour().getTourElements().get(3) instanceof org.matsim.contrib.freight.carrier.Tour.Delivery);
		assertEquals("1", route2.getTour().getStartLinkId().toString());
		assertEquals("15", ((org.matsim.contrib.freight.carrier.Tour.Pickup)route2.getTour().getTourElements().get(1)).getLocation().toString());
		assertEquals("22", ((org.matsim.contrib.freight.carrier.Tour.Delivery)route2.getTour().getTourElements().get(3)).getLocation().toString());
		assertEquals("1", route2.getTour().getEndLinkId().toString());
	}

}
