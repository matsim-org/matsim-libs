package playground.mzilske.vbb;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.OTFVisConfigGroup.ColoringScheme;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.spt.GraphPath;

public class RunWithOpenTripPlanner {


	static final String CRS = "EPSG:3395";
	

	private GeotoolsTransformation ct = new GeotoolsTransformation(TransformationFactory.WGS84, CRS);
	private static Population population;

	private Graph graph;

	int nonefound = 0;
	
	int npersons = 0;

	private GraphServiceImpl graphservice = new GraphServiceImpl() {
		public Graph getGraph(String routerId) { return graph; }
	};

	private RetryingPathServiceImpl pathservice = new RetryingPathServiceImpl();

	private GenericAStar sptService = new GenericAStar();

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		new RunWithOpenTripPlanner().convert();
	}



	public RunWithOpenTripPlanner() throws IOException, ClassNotFoundException {
		File path = new File("/tmp/graph-bundle/Graph.obj");
		graph = Graph.load(path, Graph.LoadLevel.DEBUG);
		pathservice.setGraphService(graphservice);
		pathservice.setSptService(sptService);
	}



	private void convert() {
		// final Scenario scenario = readScenario();
		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// new NetworkCleaner().run(scenario.getNetwork());
		System.out.println("Scenario has " + scenario.getNetwork().getLinks().size() + " links.");
		scenario.getConfig().controler().setMobsim("qsim");
		scenario.getConfig().addQSimConfigGroup(new QSimConfigGroup());
		scenario.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
		scenario.getConfig().getQSimConfigGroup().setSnapshotPeriod(1);
		scenario.getConfig().getQSimConfigGroup().setRemoveStuckVehicles(false);
		scenario.getConfig().otfVis().setColoringScheme(ColoringScheme.gtfs);
		scenario.getConfig().otfVis().setDrawTransitFacilities(false);
		scenario.getConfig().transitRouter().setMaxBeelineWalkConnectionDistance(1.0);
		//	new NetworkWriter(scenario.getNetwork()).write("/Users/zilske/gtfs-bvg/network.xml");
		//	new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("/Users/zilske/gtfs-bvg/transit-schedule.xml");
		//	new VehicleWriterV1(((ScenarioImpl) scenario).getVehicles()).writeFile("/Users/zilske/gtfs-bvg/transit-vehicles.xml");

		double minX=13.1949;
		double maxX=13.5657;
		double minY=52.3926;
		double maxY=52.6341;

		
		population = scenario.getPopulation();
		for (int i=0; i<1000; ++i) {
			Coord source = new CoordImpl(minX + Math.random() * (maxX - minX), minY + Math.random() * (maxY - minY));
			Coord sink = new CoordImpl(minX + Math.random() * (maxX - minX), minY + Math.random() * (maxY - minY));
			Person person = population.getFactory().createPerson(new IdImpl(Integer.toString(i)));
			Plan plan = population.getFactory().createPlan();
			plan.addActivity(createHome(source));
			plan.addLeg(createLeg(source, sink));
			plan.addActivity(createWork(sink));
			plan.addLeg(createLeg(sink, source));
			plan.addActivity(createHome(source));
			person.addPlan(plan);
			population.addPerson(person);
		}

		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.run();


		//		EventsManager events = EventsUtils.createEventsManager();
		//		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		//		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		//		OTFClientLive.run(scenario.getConfig(), server);
		//		qSim.run();




	}

	private Leg createLeg(Coord source, Coord sink) {
		TraverseModeSet modeSet = new TraverseModeSet();
		modeSet.setWalk(true);
		modeSet.setBicycle(true);
		modeSet.setFerry(true);
		modeSet.setTrainish(true);
		modeSet.setBusish(true);
		modeSet.setTransit(true);
		RoutingRequest options = new RoutingRequest(modeSet);
		options.setWalkBoardCost(3 * 60); // override low 2-4 minute values
		// TODO LG Add ui element for bike board cost (for now bike = 2 * walk)
		options.setBikeBoardCost(3 * 60 * 2);
		// there should be a ui element for walk distance and optimize type
		options.setOptimize(OptimizeType.QUICK);
		options.setMaxWalkDistance(Double.MAX_VALUE);

		Date when = null;
		try {
			when = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2011-10-15 10:00:00");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		options.setDateTime(when);

		options.from =  source.getY() +"," +source.getX();
		options.to   =  sink.getY()+ "," +  sink.getX();
		options.numItineraries = 1;
		System.out.println("--------");
		System.out.println("Path from " + options.from + " to " + options.to + " at " + when);
		System.out.println("\tModes: " + modeSet);
		System.out.println("\tOptions: " + options);
		
		List<GraphPath> paths = null;
		try {
			paths = pathservice.getPaths(options);
		} catch (VertexNotFoundException e) {
			System.out.println("None found " + nonefound++);
		}
		Leg leg = population.getFactory().createLeg(TransportMode.pt);
		if (paths != null) {
			GraphPath path = paths.get(0);
			path.dump();
		} else {
			System.out.println("None found " + nonefound++);
		}
		System.out.println("---------" + npersons++);
		
		// convert path to leg.
		
		return leg;
	}

	private Activity createWork(Coord workLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(17*60*60);
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(9*60*60);
		return activity;
	}

//	private static Scenario readScenario() {
//		// GtfsConverter gtfs = new GtfsConverter("/Users/zilske/Documents/torino", new GeotoolsTransformation("WGS84", CRS));
//		Config config = ConfigUtils.createConfig();
//		config.global().setCoordinateSystem(CRS);
//		config.controler().setLastIteration(0);
//		config.scenario().setUseVehicles(true);
//		config.scenario().setUseTransit(true);
//		Scenario scenario = ScenarioUtils.createScenario(config);
//		GtfsConverter gtfs = new GtfsConverter("/Users/zilske/gtfs-bvg", scenario, new GeotoolsTransformation("WGS84", CRS));
//		gtfs.setCreateShapedNetwork(false); // Shaped network doesn't work yet.
//		//		gtfs.setDate(20110711);
//		gtfs.convert();
//		return scenario;
//	}



}
