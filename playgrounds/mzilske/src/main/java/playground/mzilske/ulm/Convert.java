package playground.mzilske.ulm;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.OTFVisConfigGroup.ColoringScheme;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import playground.mzilske.vbb.GtfsConverter;

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
		scenario.getConfig().qsim().setSnapshotStyle("queue");
		scenario.getConfig().qsim().setSnapshotPeriod(1);
		scenario.getConfig().qsim().setRemoveStuckVehicles(false);
		scenario.getConfig().otfVis().setColoringScheme(ColoringScheme.gtfs);
		scenario.getConfig().otfVis().setDrawTransitFacilities(false);
		scenario.getConfig().transitRouter().setMaxBeelineWalkConnectionDistance(1.0);
		new NetworkWriter(scenario.getNetwork()).write("/Users/michaelzilske/gtfs-ulm/network.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("/Users/michaelzilske/gtfs-ulm/transit-schedule.xml");
		new VehicleWriterV1(((ScenarioImpl) scenario).getVehicles()).writeFile("/Users/michaelzilske/gtfs-ulm/transit-vehicles.xml");		
	}
	
	private static Scenario readScenario() {
		// GtfsConverter gtfs = new GtfsConverter("/Users/zilske/Documents/torino", new GeotoolsTransformation("WGS84", CRS));
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(CRS);
		config.controler().setLastIteration(0);
		config.scenario().setUseVehicles(true);
		config.scenario().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		GtfsConverter gtfs = new GtfsConverter("/Users/michaelzilske/gtfs-ulm", scenario, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S));
		gtfs.setCreateShapedNetwork(false); // Shaped network doesn't work yet.
		gtfs.setDate(20130824);
		gtfs.convert();
		return scenario;
	}



}
