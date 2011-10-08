package freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mzilske.freight.carrier.CarrierShipment;
import playground.mzilske.freight.carrier.CarrierUtils;
import playground.mzilske.freight.carrier.CarrierVehicle;
import playground.mzilske.freight.carrier.Tour;
import playground.mzilske.freight.carrier.Tour.Delivery;
import playground.mzilske.freight.carrier.Tour.Pickup;
import vrp.algorithms.ruinAndRecreate.constraints.CapacityConstraint;
import vrp.algorithms.ruinAndRecreate.factories.StandardRuinAndRecreateFactory;
import vrp.basics.CrowFlyDistance;
import vrp.basics.RandomNumberGeneration;
import vrp.basics.SingleDepotSolutionFactoryImpl;

public class RRSingleDepotVRPSolverTest extends TestCase{
	
	class MyVRPSolverFactory implements VRPSolverFactory{

		@Override
		public VRPSolver createSolver(Collection<CarrierShipment> shipments,Collection<CarrierVehicle> carrierVehicles, Network network) {
			RRSingleDepotVRPSolver solver = new RRSingleDepotVRPSolver(shipments, carrierVehicles, network);
			CrowFlyDistance costs = new CrowFlyDistance();
			costs.speed = 1;
			StandardRuinAndRecreateFactory ruinAndRecreateFactory = new StandardRuinAndRecreateFactory();
			solver.setRuinAndRecreateFactory(ruinAndRecreateFactory);
			solver.setCosts(costs);
			solver.setConstraints(new CapacityConstraint());
			solver.setIniSolutionFactory(new SingleDepotSolutionFactoryImpl());
			solver.setnOfWarmupIterations(20);
			solver.setnOfIterations(50);
			return solver;
		}
	}
	
	
	Collection<CarrierVehicle> vehicles;
	
	Collection<CarrierShipment> shipments;
	
	Scenario scenario;
	
	public void setUp(){
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
		shipments = new ArrayList<CarrierShipment>();
		vehicles = new ArrayList<CarrierVehicle>();
		createTestNetwork();
		RandomNumberGeneration.reset();
	}
	
	private void createTestNetwork() {
		Node n1 = scenario.getNetwork().getFactory().createNode(makeId("(0,0)"), makeCoord(0,0));
		scenario.getNetwork().addNode(n1);
		Node n2 = scenario.getNetwork().getFactory().createNode(makeId("(0,10)"), makeCoord(0,10));
		scenario.getNetwork().addNode(n2);
		Node n3 = scenario.getNetwork().getFactory().createNode(makeId("(10,10)"), makeCoord(10,10));
		scenario.getNetwork().addNode(n3);
		Node n4 = scenario.getNetwork().getFactory().createNode(makeId("(10,0)"), makeCoord(10,0));
		scenario.getNetwork().addNode(n4);
		
		Link l1 = scenario.getNetwork().getFactory().createLink(makeId("1"), n1, n2);
		scenario.getNetwork().addLink(l1);
		Link l2 = scenario.getNetwork().getFactory().createLink(makeId("2"), n2, n3);
		scenario.getNetwork().addLink(l2);
		Link l3 = scenario.getNetwork().getFactory().createLink(makeId("3"), n3, n4);
		scenario.getNetwork().addLink(l3);
		Link l4 = scenario.getNetwork().getFactory().createLink(makeId("4"), n4, n1);
		scenario.getNetwork().addLink(l4);
		
	}

	private Coord makeCoord(int i, int j) {
		return new CoordImpl(i,j);
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}

	public void testSolveWithNoShipments(){
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"), makeId("vehicleLocation"));
		vehicle.setCapacity(10);
		vehicles.add(vehicle);
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		assertTrue(tours.isEmpty());
	}
	
	public void testSolveWithNoVehicles(){
		vehicles.clear();
		shipments.add(makeShipment("depotLocation","customerLocation",20));
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		assertTrue(tours.isEmpty());
	}
	
	public void testVrpSolutionSolutionSize(){
		CarrierShipment s1 = makeShipment("1", "2", 10);
		CarrierShipment s2 = makeShipment("1", "3", 10);
		shipments.add(s1);
		shipments.add(s2);
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"), makeId("1"));
		vehicle.setCapacity(20);
		vehicles.add(vehicle);
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		assertEquals(1, tours.size());
	}
	
	public void testVrpSolutionToMatsimTourTransformation(){
		CarrierShipment s1 = makeShipment("1", "2", 10);
		CarrierShipment s2 = makeShipment("1", "3", 10);
		shipments.add(s1);
		shipments.add(s2);
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"), makeId("1"));
		vehicle.setCapacity(20);
		vehicles.add(vehicle);
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		Tour tour = tours.iterator().next();
		assertEquals(tour.getStartLinkId(), makeId("1"));
		assertEquals(tour.getEndLinkId(), makeId("1"));
	}
	
	public void testVrpSolutionToMatsimTourTransformation_NoShipments(){
		CarrierShipment s1 = makeShipment("1", "2", 10);
		CarrierShipment s2 = makeShipment("1", "3", 10);
		shipments.add(s1);
		shipments.add(s2);
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"), makeId("1"));
		vehicle.setCapacity(20);
		vehicles.add(vehicle);
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		Tour tour = tours.iterator().next();
		assertEquals(tour.getShipments().size(), 2);
	}
	
	public void testVrpSolutionToMatsimTourTransformation_NoTourElements(){
		CarrierShipment s1 = makeShipment("1", "2", 10);
		CarrierShipment s2 = makeShipment("1", "3", 10);
		shipments.add(s1);
		shipments.add(s2);
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"), makeId("1"));
		vehicle.setCapacity(20);
		vehicles.add(vehicle);
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		Tour tour = tours.iterator().next();
		assertEquals(tour.getTourElements().size(), 4);
	}
	
	public void testVrpSolutionToMatsimTourTransformation_TourElementsForDeliveryTour(){
		CarrierShipment s1 = makeShipment("1", "2", 10);
		CarrierShipment s2 = makeShipment("1", "3", 10);
		shipments.add(s1);
		shipments.add(s2);
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"), makeId("1"));
		vehicle.setCapacity(20);
		vehicles.add(vehicle);
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		Tour tour = tours.iterator().next();
		assertEquals(tour.getTourElements().get(0).getClass(),Pickup.class);
		assertEquals(tour.getTourElements().get(0).getLocation(),makeId("1"));
		assertEquals(tour.getTourElements().get(1).getClass(),Pickup.class);
		assertEquals(tour.getTourElements().get(1).getLocation(),makeId("1"));
		assertEquals(tour.getTourElements().get(2).getClass(),Delivery.class);
		assertEquals(tour.getTourElements().get(2).getLocation(),makeId("3"));
		assertEquals(tour.getTourElements().get(3).getClass(),Delivery.class);
		assertEquals(tour.getTourElements().get(3).getLocation(),makeId("2"));
	}
	
	public void testVrpSolutionToMatsimTourTransformation_TourElementsForPickupTour(){
		CarrierShipment s1 = makeShipment("2", "1", 10);
		CarrierShipment s2 = makeShipment("3", "1", 10);
		shipments.add(s1);
		shipments.add(s2);
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"), makeId("1"));
		vehicle.setCapacity(20);
		vehicles.add(vehicle);
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		Tour tour = tours.iterator().next();
		assertEquals(tour.getTourElements().get(0).getClass(),Pickup.class);
		assertEquals(tour.getTourElements().get(0).getLocation(),makeId("3"));
		assertEquals(tour.getTourElements().get(1).getClass(),Pickup.class);
		assertEquals(tour.getTourElements().get(1).getLocation(),makeId("2"));
		assertEquals(tour.getTourElements().get(2).getClass(),Delivery.class);
		assertEquals(tour.getTourElements().get(2).getLocation(),makeId("1"));
		assertEquals(tour.getTourElements().get(3).getClass(),Delivery.class);
		assertEquals(tour.getTourElements().get(3).getLocation(),makeId("1"));
	}
	
	public void testVrpSolutionToMatsimTourTransformation_TourElementsForEnRoutePickupAndDeliveryTour(){
		CarrierShipment s1 = makeShipment("2", "4", 10);
//		CarrierShipment s2 = makeShipment("3", "1", 10);
		shipments.add(s1);
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"), makeId("1"));
		vehicle.setCapacity(20);
		vehicles.add(vehicle);
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		Tour tour = tours.iterator().next();
		assertEquals(tour.getTourElements().get(0).getClass(),Pickup.class);
		assertEquals(tour.getTourElements().get(0).getLocation(),makeId("2"));
		assertEquals(tour.getTourElements().get(1).getClass(),Delivery.class);
		assertEquals(tour.getTourElements().get(1).getLocation(),makeId("4"));
	}
	
	public void testVrpSolutionToMatsimTourTransformation_TourElementsForMixedEnRoutePickupAndDeliveryTour(){
		CarrierShipment s1 = makeShipment("2", "4", 10);
		CarrierShipment s2 = makeShipment("3", "1", 10);
		shipments.add(s1);
		shipments.add(s2);
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"), makeId("1"));
		vehicle.setCapacity(20);
		vehicles.add(vehicle);
		Collection<Tour> tours = new MyVRPSolverFactory().createSolver(shipments, vehicles, scenario.getNetwork()).solve();
		Tour tour = tours.iterator().next();
		assertEquals(tour.getTourElements().get(0).getClass(),Pickup.class);
		assertEquals(tour.getTourElements().get(0).getLocation(),makeId("2"));
		assertEquals(tour.getTourElements().get(1).getClass(),Pickup.class);
		assertEquals(tour.getTourElements().get(1).getLocation(),makeId("3"));
		assertEquals(tour.getTourElements().get(2).getClass(),Delivery.class);
		assertEquals(tour.getTourElements().get(2).getLocation(),makeId("4"));
		assertEquals(tour.getTourElements().get(3).getClass(),Delivery.class);
		assertEquals(tour.getTourElements().get(3).getLocation(),makeId("1"));
	}
	
	
	private CarrierShipment makeShipment(String from, String to, int size) {
		return CarrierUtils.createShipment(makeId(from), makeId(to), size, 0.0, Double.MAX_VALUE, 0.0, Double.MAX_VALUE);
	}

}
