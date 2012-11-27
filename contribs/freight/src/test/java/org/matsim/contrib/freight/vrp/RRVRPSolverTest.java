package org.matsim.contrib.freight.vrp;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierFactory;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateStandardAlgorithmFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesCostAndTWs;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesLocalActInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.CalculatesShipmentInsertion;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.StandardRouteAgentFactory;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.TourCost;
import org.matsim.contrib.freight.vrp.basics.Coordinate;
import org.matsim.contrib.freight.vrp.basics.CrowFlyCosts;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingProblemType;
import org.matsim.contrib.freight.vrp.utils.RandomNumberGeneration;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

public class RRVRPSolverTest extends TestCase {

	class MyVRPSolverFactory implements MatsimVrpSolverFactory {

		@Override
		public MatsimVrpSolver createSolver(
				Carrier carrier,
				Network network, TourCost tourCost,
				VehicleRoutingCosts costs) {
			RouteAgentFactory spFactory = new StandardRouteAgentFactory(new CalculatesShipmentInsertion(costs, new CalculatesLocalActInsertion(costs)), new CalculatesCostAndTWs(costs));
			MatsimVrpSolverImpl solver = new MatsimVrpSolverImpl(carrier,costs);
			RuinAndRecreateStandardAlgorithmFactory ruinAndRecreateFactory = new RuinAndRecreateStandardAlgorithmFactory(
					spFactory);
			solver.setVrpSolverFactory(ruinAndRecreateFactory);
			return solver;
		}
	}

	Collection<CarrierVehicle> vehicles;

	Collection<CarrierShipment> shipments;

	Scenario scenario;

	private CrowFlyCosts costs;

	private TourCost tourCost;

	@Override
	public void setUp() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
		shipments = new ArrayList<CarrierShipment>();
		vehicles = new ArrayList<CarrierVehicle>();
		createTestNetwork();
		costs = new CrowFlyCosts(
				new org.matsim.contrib.freight.vrp.basics.Locations() {

					@Override
					public Coordinate getCoord(String id) {
						Coord coord = scenario.getNetwork().getLinks()
								.get(makeId(id)).getCoord();
						return makeCoordinate(coord);
					}

					private Coordinate makeCoordinate(Coord coord) {
						return new Coordinate(coord.getX(), coord.getY());
					}

				});
		costs.speed = 18;
		costs.detourFactor = 1.2;

		tourCost = new TourCost() {

			@Override
			public double getTourCost(TourImpl tour, Driver driver,
					Vehicle vehicle) {
				return 100 + tour.tourData.transportCosts;
			}

		};

		RandomNumberGeneration.reset();
	}

	private void createTestNetwork() {
		Node n1 = scenario.getNetwork().getFactory()
				.createNode(makeId("(0,0)"), makeCoord(0, 0));
		scenario.getNetwork().addNode(n1);
		Node n2 = scenario.getNetwork().getFactory()
				.createNode(makeId("(0,10)"), makeCoord(0, 10));
		scenario.getNetwork().addNode(n2);
		Node n3 = scenario.getNetwork().getFactory()
				.createNode(makeId("(10,10)"), makeCoord(10, 10));
		scenario.getNetwork().addNode(n3);
		Node n4 = scenario.getNetwork().getFactory()
				.createNode(makeId("(10,0)"), makeCoord(10, 0));
		scenario.getNetwork().addNode(n4);

		Link l1 = scenario.getNetwork().getFactory()
				.createLink(makeId("1"), n1, n2);
		scenario.getNetwork().addLink(l1);
		Link l2 = scenario.getNetwork().getFactory()
				.createLink(makeId("2"), n2, n3);
		scenario.getNetwork().addLink(l2);
		Link l3 = scenario.getNetwork().getFactory()
				.createLink(makeId("3"), n3, n4);
		scenario.getNetwork().addLink(l3);
		Link l4 = scenario.getNetwork().getFactory()
				.createLink(makeId("4"), n4, n1);
		scenario.getNetwork().addLink(l4);

	}

	private Coord makeCoord(int i, int j) {
		return new CoordImpl(i, j);
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}

	public void testSolveWithNoShipments() {
		CarrierVehicle vehicle = new CarrierVehicle(makeId("vehicle"),makeId("vehicleLocation"));
		vehicle.setVehicleType(new CarrierVehicleType(makeId("standard")));
		vehicle.setCapacity(10);
		vehicles.add(vehicle);
		Carrier carrier = new CarrierFactory().createCarrier("c", "l1");
		carrier.setCarrierCapabilities(new CarrierFactory().createCapabilities());
		carrier.getCarrierCapabilities().getCarrierVehicles().add(vehicle);
		carrier.getShipments().addAll(shipments);
		Collection<ScheduledTour> tours = new MyVRPSolverFactory().createSolver(carrier, scenario.getNetwork(), tourCost,costs).solve();
		assertTrue(tours.isEmpty());
	}

	public void testSolveWithNoVehicles() {
		vehicles.clear();
		shipments.add(makeShipment("depotLocation", "customerLocation", 20));
		Carrier carrier = new CarrierFactory().createCarrier("c", "l1");
		carrier.setCarrierCapabilities(new CarrierFactory().createCapabilities());
		carrier.getShipments().addAll(shipments);
		try {
			Collection<ScheduledTour> tours = new MyVRPSolverFactory().createSolver(carrier, scenario.getNetwork(), tourCost,costs).solve();
			assertTrue(false);
		} catch (IllegalStateException e) {
			assertTrue(true);
		}

	}

	private CarrierShipment makeShipment(String from, String to, int size) {
		return new CarrierFactory().createShipment(from, to, size, 0.0,
				Double.MAX_VALUE, 0.0, Double.MAX_VALUE);
	}

}
