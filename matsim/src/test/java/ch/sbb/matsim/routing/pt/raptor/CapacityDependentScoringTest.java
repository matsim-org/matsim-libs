package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.ExecutionData.DepartureData;
import ch.sbb.matsim.routing.pt.raptor.ExecutionData.LineData;
import ch.sbb.matsim.routing.pt.raptor.ExecutionData.RouteData;
import ch.sbb.matsim.routing.pt.raptor.ExecutionData.StopData;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mrieser / Simunto
 */
public class CapacityDependentScoringTest {

	@Test
	public void testScoring() {
		Fixture f = new Fixture();

		double normalScore = calcScore(f, false);
		double capDepScore = calcScore(f, true);

		System.out.println("normal score: " + normalScore);
		System.out.println("capacity dependent score: " + capDepScore);

		// in the normal case, it's a 15min trips at full cost, so it should be -6 * (1/4) = -1.5
		// in the capacity dependent case, the vehicle is empty, thus the cost should only be 0.7 * original cost => -1.05

		Assert.assertEquals(-1.5, normalScore, 1e-7);
		Assert.assertEquals(-1.05, capDepScore, 1e-7);
	}

	private double calcScore(Fixture f, boolean capacityDependent) {
		Config config = f.config;
		Network network = f.scenario.getNetwork();
		ScoringParametersForPerson parameters = new SubpopulationScoringParameters(f.scenario);

		ExecutionData execData = null;
		CapacityDependentInVehicleCostCalculator inVehicleCostCalculator = null;
		if (capacityDependent) {
			execData = new ExecutionData();
			inVehicleCostCalculator = new CapacityDependentInVehicleCostCalculator(0.7, 0.3, 1.2, 0.8);
		}

		ExecutionData finalExecData = execData;
		CapacityDependentInVehicleCostCalculator finalInVehicleCostCalculator = inVehicleCostCalculator;

		ScoringFunctionFactory testSFF = new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, network, config.transit().getTransitModes()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				if (capacityDependent) {
					scoringFunctionAccumulator.addScoringFunction(new CapacityDependentTripScoringFunction(person, params, config.transit().getTransitModes(), finalExecData, finalInVehicleCostCalculator));
				}

				return scoringFunctionAccumulator;
			}
		};

		Population population = f.scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		Person person = pf.createPerson(Id.create("1", Person.class));

		if (capacityDependent) {
			Vehicles transitVehicles = f.scenario.getTransitVehicles();
			VehicleType vehType = VehicleUtils.createVehicleType(Id.create("bus", VehicleType.class));
			vehType.getCapacity().setSeats(5);
			Vehicle veh = VehicleUtils.createVehicle(Id.create("v1", Vehicle.class), vehType);
			Id<Departure> departureId = Id.create(1, Departure.class);
			LineData lineData = new LineData();
			execData.lineData.put(f.fastLineId, lineData);

			RouteData routeData = new RouteData(f.scenario.getTransitSchedule().getTransitLines().get(f.fastLineId).getRoutes().get(f.fastRouteId));
			lineData.routeData.put(f.fastRouteId, routeData);
			routeData.vehicles.put(departureId, veh);

			StopData stopDataA = new StopData();
			DepartureData depDataA1 = stopDataA.getOrCreate(departureId);
			depDataA1.vehDepTime = 7*3600;
			routeData.stopData.put(f.stopAId, stopDataA);
			StopData stopDataB = new StopData();
			DepartureData depDataB1 = stopDataB.getOrCreate(departureId);
			depDataB1.vehDepTime = 7*3600 + 5*60;
			routeData.stopData.put(f.stopBId, stopDataB);
			StopData stopDataC = new StopData();
			DepartureData depDataC1 = stopDataC.getOrCreate(departureId);
			depDataC1.vehDepTime = 7*3600 + 10*60;
			routeData.stopData.put(f.stopCId, stopDataC);
			StopData stopDataD = new StopData();
			DepartureData depDataD1 = stopDataD.getOrCreate(departureId);
			depDataD1.vehDepTime = 7*3600 + 15*60;
			routeData.stopData.put(f.stopDId, stopDataD);

			execData.lastUsedDeparturePerPerson.put(person.getId(), departureId);
		}

		ScoringFunction sf = testSFF.createNewScoringFunction(person);

		TransitPassengerRoute ptRoute = new DefaultTransitPassengerRoute(Id.create("aa", Link.class), Id.create("dd", Link.class), f.stopAId, f.stopDId, f.fastLineId, f.fastRouteId);
		Leg leg = pf.createLeg("pt");
		leg.setRoute(ptRoute);
		leg.setDepartureTime(7*3600);
		leg.setTravelTime(15*60);

		sf.handleLeg(leg);

		return sf.getScore();
	}

	private static class Fixture {
		/* Main idea of scenario: two parallel lines, one a bit slower than the other.
		   Normally, agents should prefer the faster line, but if that one is over capacity, then
		   agents should start switching to the slower line.
		 */

		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario(this.config);

		final Id<TransitStopFacility> stopAId = Id.create("A", TransitStopFacility.class);
		final Id<TransitStopFacility> stopBId = Id.create("B", TransitStopFacility.class);
		final Id<TransitStopFacility> stopCId = Id.create("C", TransitStopFacility.class);
		final Id<TransitStopFacility> stopDId = Id.create("D", TransitStopFacility.class);

		final TransitStopFacility stopA;
		final TransitStopFacility stopB;
		final TransitStopFacility stopC;
		final TransitStopFacility stopD;

		final Id<TransitLine> fastLineId = Id.create("fast", TransitLine.class);
		final Id<TransitLine> slowLineId = Id.create("slow", TransitLine.class);

		final Id<TransitRoute> fastRouteId = Id.create("fast", TransitRoute.class);
		final Id<TransitRoute> slowRouteId = Id.create("slow", TransitRoute.class);

		public Fixture() {
			// network

			Network network = this.scenario.getNetwork();
			NetworkFactory nf = network.getFactory();

			Node nodeA = nf.createNode(Id.create("a", Node.class), new Coord(1000, 1000));
			Node nodeB = nf.createNode(Id.create("b", Node.class), new Coord(3000, 1000));
			Node nodeC = nf.createNode(Id.create("c", Node.class), new Coord(5000, 1000));
			Node nodeD = nf.createNode(Id.create("d", Node.class), new Coord(7000, 1000));

			network.addNode(nodeA);
			network.addNode(nodeB);
			network.addNode(nodeC);
			network.addNode(nodeD);

			Link linkAA = NetworkUtils.createLink(Id.create("aa", Link.class), nodeA, nodeA, network, 3000, 20, 2000, 1);
			Link linkAB = NetworkUtils.createLink(Id.create("ab", Link.class), nodeA, nodeB, network, 3000, 20, 2000, 1);
			Link linkBC = NetworkUtils.createLink(Id.create("bc", Link.class), nodeB, nodeC, network, 3000, 20, 2000, 1);
			Link linkCD = NetworkUtils.createLink(Id.create("cd", Link.class), nodeC, nodeD, network, 3000, 20, 2000, 1);

			network.addLink(linkAA);
			network.addLink(linkAB);
			network.addLink(linkBC);
			network.addLink(linkCD);

			// transit vehicles

			Vehicles transitVehicles = this.scenario.getTransitVehicles();
			VehiclesFactory vf = transitVehicles.getFactory();

			VehicleType vehType = vf.createVehicleType(Id.create("some-bus", VehicleType.class));
			vehType.getCapacity().setSeats(5);
			vehType.getCapacity().setStandingRoom(0);
			transitVehicles.addVehicleType(vehType);

			Vehicle[] fastVehicles = new Vehicle[10];
			Vehicle[] slowVehicles = new Vehicle[10];
			for (int i = 0; i < 10; i++) {
				fastVehicles[i] = vf.createVehicle(Id.create("fast" + i, Vehicle.class), vehType);
				slowVehicles[i] = vf.createVehicle(Id.create("slow" + i, Vehicle.class), vehType);
				transitVehicles.addVehicle(fastVehicles[i]);
				transitVehicles.addVehicle(slowVehicles[i]);
			}

			// transit schedule

			TransitSchedule schedule = this.scenario.getTransitSchedule();
			TransitScheduleFactory sf = schedule.getFactory();

			this.stopA = sf.createTransitStopFacility(this.stopAId, new Coord(1000, 1000), false);
			this.stopB = sf.createTransitStopFacility(this.stopBId, new Coord(3000, 1000), false);
			this.stopC = sf.createTransitStopFacility(this.stopCId, new Coord(5000, 1000), false);
			this.stopD = sf.createTransitStopFacility(this.stopDId, new Coord(7000, 1000), false);

			this.stopA.setLinkId(linkAA.getId());
			this.stopB.setLinkId(linkAB.getId());
			this.stopC.setLinkId(linkBC.getId());
			this.stopD.setLinkId(linkCD.getId());

			schedule.addStopFacility(this.stopA);
			schedule.addStopFacility(this.stopB);
			schedule.addStopFacility(this.stopC);
			schedule.addStopFacility(this.stopD);

			{ // fast line
				TransitLine fastLine = sf.createTransitLine(this.fastLineId);
				List<TransitRouteStop> fastStops = new ArrayList<>();
				fastStops.add(sf.createTransitRouteStop(this.stopA, Time.getUndefinedTime(), 0.0));
				fastStops.add(sf.createTransitRouteStop(this.stopB, 5 * 60 - 30, 5 * 60));
				fastStops.add(sf.createTransitRouteStop(this.stopC, 10 * 60 - 30, 10 * 60));
				fastStops.add(sf.createTransitRouteStop(this.stopD, 15 * 60 - 30, Time.getUndefinedTime()));

				NetworkRoute route = RouteUtils.createNetworkRoute(List.of(linkAA.getId(), linkAB.getId(), linkBC.getId(), linkCD.getId()), network);

				TransitRoute fastRoute = sf.createTransitRoute(this.fastRouteId, route, fastStops, "bus");
				fastLine.addRoute(fastRoute);
				schedule.addTransitLine(fastLine);

				for (int i = 0; i < 10; i++) {
					Departure dep = sf.createDeparture(Id.create("f" + i, Departure.class), 7 * 3600 + i * 600);
					dep.setVehicleId(fastVehicles[i].getId());
					fastRoute.addDeparture(dep);
				}
			}

			{ // slow line
				TransitLine slowLine = sf.createTransitLine(this.slowLineId);
				List<TransitRouteStop> slowStops = new ArrayList<>();
				slowStops.add(sf.createTransitRouteStop(this.stopA, Time.getUndefinedTime(), 0.0));
				slowStops.add(sf.createTransitRouteStop(this.stopB, 7 * 60 - 30, 7 * 60));
				slowStops.add(sf.createTransitRouteStop(this.stopC, 14 * 60 - 30, 14 * 60));
				slowStops.add(sf.createTransitRouteStop(this.stopD, 21 * 60 - 30, Time.getUndefinedTime()));

				NetworkRoute route = RouteUtils.createNetworkRoute(List.of(linkAA.getId(), linkAB.getId(), linkBC.getId(), linkCD.getId()), network);

				TransitRoute slowRoute = sf.createTransitRoute(this.slowRouteId, route, slowStops, "bus");
				slowLine.addRoute(slowRoute);
				schedule.addTransitLine(slowLine);

				for (int i = 0; i < 10; i++) {
					Departure dep = sf.createDeparture(Id.create("s" + i, Departure.class), 7 * 3600 + 60 + i * 600);
					dep.setVehicleId(slowVehicles[i].getId());
					slowRoute.addDeparture(dep);
				}
			}
		}
	}

}
