package playground.mzilske.vbb;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.OTFVisConfigGroup.ColoringScheme;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

public class Convert {
	

	static final String CRS = "EPSG:3395";
	private static Population population;
	
	public static void main(String[] args) {
		new Convert().convert();
	}

	private void convert() {
		final Scenario scenario = readScenario();
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
		new NetworkWriter(scenario.getNetwork()).write("/Users/zilske/gtfs-bvg/network.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("/Users/zilske/gtfs-bvg/transit-schedule.xml");
		new VehicleWriterV1(((ScenarioImpl) scenario).getVehicles()).writeFile("/Users/zilske/gtfs-bvg/transit-vehicles.xml");
		
//		double minX=13.1949;
//		double maxX=13.5657;
//		double minY=52.3926;
//		double maxY=52.6341;
//		
//		population = scenario.getPopulation();
//		GeotoolsTransformation ct = new GeotoolsTransformation("WGS84", CRS);
//		for (int i=0; i<1000; ++i) {
//			Coord source = ct.transform(new CoordImpl(minX + Math.random() * (maxX - minX), minY + Math.random() * (maxY - minY)));
//			Coord sink = ct.transform(new CoordImpl(minX + Math.random() * (maxX - minX), minY + Math.random() * (maxY - minY)));
//			Person person = population.getFactory().createPerson(new IdImpl(Integer.toString(i)));
//			Plan plan = population.getFactory().createPlan();
//			plan.addActivity(createHome(source));
//			plan.addLeg(createDriveLeg());
//			plan.addActivity(createWork(sink));
//			plan.addLeg(createDriveLeg());
//			plan.addActivity(createHome(source));
//			person.addPlan(plan);
//			population.addPerson(person);
//		}
//		
//		Controler controler = new Controler(scenario);
//		controler.setOverwriteFiles(true);
//		controler.run();
		
		
//		EventsManager events = EventsUtils.createEventsManager();
//		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
//		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
//		OTFClientLive.run(scenario.getConfig(), server);
//		qSim.run();
		
		
		
		
	}
	
	private Leg createDriveLeg() {
		Leg leg = population.getFactory().createLeg(TransportMode.pt);
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

	private static Scenario readScenario() {
		// GtfsConverter gtfs = new GtfsConverter("/Users/zilske/Documents/torino", new GeotoolsTransformation("WGS84", CRS));
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(CRS);
		config.controler().setLastIteration(0);
		config.scenario().setUseVehicles(true);
		config.scenario().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		// GtfsConverter gtfs = new GtfsConverter("/Users/zilske/gtfs-bvg", scenario, new GeotoolsTransformation("WGS84", CRS));
		GtfsConverter gtfs = new GtfsConverter("/Users/zilske/gtfs-bvg", scenario, new CoordinateTransformation() {
			
			@Override
			public Coord transform(Coord coord) {
				return coord;
			}
		});
		gtfs.setCreateShapedNetwork(false); // Shaped network doesn't work yet.
		//		gtfs.setDate(20110711);
		gtfs.convert();
		return scenario;
	}



}
