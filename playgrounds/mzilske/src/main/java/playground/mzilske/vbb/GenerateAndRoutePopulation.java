package playground.mzilske.vbb;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.VehicleReaderV1;

import playground.mzilske.otp.OTPTripRouterFactory;

public class GenerateAndRoutePopulation {



	private static Population population;

	private NetworkImpl network;

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		new GenerateAndRoutePopulation().convert();
	}



	public GenerateAndRoutePopulation() throws IOException, ClassNotFoundException {

	}



	private void convert() {
		// final Scenario scenario = readScenario();
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseVehicles(true);
		config.scenario().setUseTransit(true);
		config.transit().setTransitScheduleFile("/Users/zilske/gtfs-bvg/transit-schedule.xml");
		config.transit().setVehiclesFile("/Users/zilske/gtfs-bvg/transit-vehicles.xml");
		config.network().setInputFile("/Users/zilske/gtfs-bvg/network.xml");
		final Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario).readFile("/Users/zilske/gtfs-bvg/network.xml");
		new VehicleReaderV1(((ScenarioImpl) scenario).getVehicles()).readFile(config.transit().getVehiclesFile());
		new TransitScheduleReader(scenario).readFile(config.transit().getTransitScheduleFile());

		// new NetworkCleaner().run(scenario.getNetwork());
		System.out.println("Scenario has " + scenario.getNetwork().getLinks().size() + " links.");

		//	new NetworkWriter(scenario.getNetwork()).write("/Users/zilske/gtfs-bvg/network.xml");
		//	new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("/Users/zilske/gtfs-bvg/transit-schedule.xml");
		//	new VehicleWriterV1(((ScenarioImpl) scenario).getVehicles()).writeFile("/Users/zilske/gtfs-bvg/transit-vehicles.xml");

		double minX=13.1949;
		double maxX=13.5657;
		double minY=52.3926;
		double maxY=52.6341;


		population = scenario.getPopulation();
		network = (NetworkImpl) scenario.getNetwork();
		for (int i=0; i<10; ++i) {
			Coord source = new CoordImpl(minX + Math.random() * (maxX - minX), minY + Math.random() * (maxY - minY));
			Coord sink = new CoordImpl(minX + Math.random() * (maxX - minX), minY + Math.random() * (maxY - minY));
			Person person = population.getFactory().createPerson(Id.create(Integer.toString(i), Person.class));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHome(source));
			List<Leg> homeWork = createLeg(source, sink);
			for (Leg leg : homeWork) {
				plan.addLeg(leg);
			}
			plan.addActivity(createWork(sink));
			List<Leg> workHome = createLeg(sink, source);
			for (Leg leg : workHome) {
				plan.addLeg(leg);
			}
			plan.addActivity(createHome(source));
			person.addPlan(plan);
			population.addPerson(person);
		}

		final OTPTripRouterFactory trf = new OTPTripRouterFactory(scenario.getTransitSchedule(), new IdentityTransformation(), "2013-08-24");

		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(population, config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(new PlanRouter(
						trf.instantiateAndConfigureTripRouter(new RoutingContext() {

							@Override
							public TravelDisutility getTravelDisutility() {
								// TODO Auto-generated method stub
								return null;
							}

							@Override
							public TravelTime getTravelTime() {
								// TODO Auto-generated method stub
								return null;
							}
							
						}),
						((ScenarioImpl)scenario).getActivityFacilities()), scenario);
			}
		});

		new PopulationWriter(population, scenario.getNetwork()).writeV5("/Users/zilske/gtfs-bvg/population.xml");

	}

	private List<Leg> createLeg(Coord source, Coord sink) {
		Leg leg = population.getFactory().createLeg(TransportMode.pt);
		return Arrays.asList(new Leg[]{leg});
	}

	private Activity createWork(Coord workLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(17*60*60);
		((ActivityImpl) activity).setLinkId(network.getNearestLinkExactly(workLocation).getId());
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(9*60*60);
		Link link = network.getNearestLinkExactly(homeLocation);
		((ActivityImpl) activity).setLinkId(link.getId());
		return activity;
	}

}
