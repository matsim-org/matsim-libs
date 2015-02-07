package playground.mzilske.ulm;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;
import playground.mzilske.ant2014.RunResource;
import playground.mzilske.gtfs.GtfsConverter;
import playground.vsp.randomizedtransitrouter.RandomizedTransitRouterModule;

class UlmResource {
	
	private static final String CRS = "EPSG:3395";

	private static final int DATE = 20130824;

	private String wd;
	
	public UlmResource(String wd) {
		this.wd = wd;
	}

	public void convert() {
		Config config = getConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		GtfsConverter gtfs = new GtfsConverter(wd + "/swu", scenario, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S));
		gtfs.setCreateShapedNetwork(false); // Shaped network doesn't work yet.
		gtfs.setDate(DATE);
		gtfs.convert();
		// new NetworkCleaner().run(scenario.getNetwork());
		System.out.println("Scenario has " + scenario.getNetwork().getLinks().size() + " links.");

		new NetworkWriter(scenario.getNetwork()).write(wd + "/network.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(wd + "/transit-schedule.xml");
		new VehicleWriterV1(scenario.getTransitVehicles()).writeFile(wd + "/transit-vehicles.xml");
	}

	public void population() {
		Scenario scenario = getNetworkAndTransitSchedule();
		new GeneratePopulation(scenario).run();
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(wd + "/population.xml");
	}
	
	public void run() {
		Scenario scenario = loadScenario();
		Controler controler = new Controler(scenario);
        controler.addOverridingModule(new RandomizedTransitRouterModule());
		controler.run();
	}
	
	public void otfvis() {
		RunResource run = new RunResource(wd + "/output", getConfig().controler().getRunId());
		final Scenario outputScenario = run.getOutputScenario();
		OTFVis.playScenario(outputScenario);
	}
	
	private Config getConfig() {
		Config config = ConfigUtils.createConfig();
		config.global().setNumberOfThreads(8);
		config.transit().setTransitScheduleFile(wd + "/transit-schedule.xml");
		config.transit().setVehiclesFile(wd + "/transit-vehicles.xml");
		config.network().setInputFile(wd + "/network.xml");
		config.plans().setInputFile(wd + "/population.xml");
		config.global().setCoordinateSystem(CRS);
		config.scenario().setUseVehicles(true);
		config.scenario().setUseTransit(true);
		config.qsim().setSnapshotStyle("queue");
		config.qsim().setSnapshotPeriod(1);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setEndTime(35*60*60);
		config.transitRouter().setMaxBeelineWalkConnectionDistance(1.0);
		config.controler().setLastIteration(10);
		config.controler().setOutputDirectory(wd + "/output");
		
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(16*60*60);
		config.planCalcScore().addActivityParams(home);
		
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(8*60*60);
		config.planCalcScore().addActivityParams(work);
		config.planCalcScore().setWriteExperiencedPlans(true);
		
		StrategySettings change = new StrategySettings(Id.create(1, StrategySettings.class));
		change.setStrategyName("ChangeExpBeta");
		change.setWeight(0.0);
	
		StrategySettings reRoute = new StrategySettings(Id.create(2, StrategySettings.class));
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.5);
		
		StrategySettings time = new StrategySettings(Id.create(3, StrategySettings.class));
		time.setStrategyName("TimeAllocationMutator");
		time.setWeight(0.5);
	
		config.strategy().addStrategySettings(change);
		config.strategy().addStrategySettings(reRoute);
		config.strategy().addStrategySettings(time);
		
		final OTFVisConfigGroup otfvis = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
		otfvis.setColoringScheme(ColoringScheme.gtfs);
		otfvis.setDrawTransitFacilities(false);
	
		return config;
	}

	private Scenario getNetworkAndTransitSchedule() {
		Config config = getConfig();
		final Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		new VehicleReaderV1(scenario.getTransitVehicles()).readFile(config.transit().getVehiclesFile());
		new TransitScheduleReader(scenario).readFile(config.transit().getTransitScheduleFile());
		return scenario;
	}
	
	private Scenario loadScenario() {
		Config config = getConfig();
        return ScenarioUtils.loadScenario(config);
	}

    void compareOutcome() {
        TransitSchedule transitSchedule = readTransitSchedule();
        System.out.println(transitSchedule.getTransitLines().size());
        Population population0 = readPopulation(0);
        Population population10 = readPopulation(10);
        System.out.println(population0.getPersons().size());
        System.out.println(population10.getPersons().size());
    }

    private TransitSchedule readTransitSchedule() {
        Config config = ConfigUtils.createConfig();
        config.scenario().setUseTransit(true);
        Scenario scenario = ScenarioUtils.createScenario(config);
        new TransitScheduleReader(scenario).readFile(wd + "/output/output_transitSchedule.xml.gz");
        return scenario.getTransitSchedule();
    }

    private Population readPopulation(int it) {
        Config config = ConfigUtils.createConfig();
        config.scenario().setUseTransit(true);
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimPopulationReader(scenario).readFile(wd + "/output/ITERS/it."+it+"/"+it+".experienced_plans.xml.gz");
        return scenario.getPopulation();
    }

}
