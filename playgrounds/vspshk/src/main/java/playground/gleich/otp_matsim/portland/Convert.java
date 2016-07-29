package playground.gleich.otp_matsim.portland;


import com.conveyal.gtfs.GTFSFeed;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

import java.time.LocalDate;

/**
 * copy of playground.mzilske/vbb/Convert
 * @author gleich
 *
 */
public class Convert {
	

	static final String CRS = "EPSG:2991";
	private static Population population;
	
	public static void main(String[] args) {
		new Convert().convert();
	}

	private void convert() {
		final Scenario scenario = readScenario();
		// new NetworkCleaner().run(scenario.getNetwork());
		System.out.println("Scenario has " + scenario.getNetwork().getLinks().size() + " links.");
		scenario.getConfig().controler().setMobsim("qsim");
		scenario.getConfig().qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		scenario.getConfig().qsim().setSnapshotPeriod(1);
		scenario.getConfig().qsim().setRemoveStuckVehicles(false);
		ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setColoringScheme(ColoringScheme.gtfs);
		ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawTransitFacilities(false);
		scenario.getConfig().transitRouter().setMaxBeelineWalkConnectionDistance(1.0);
//		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
//			if (scenario.getNetwork().getLinks().get(facility.getId()) == null) {
//				throw new RuntimeException();
//			}
//		}
		new NetworkWriter(scenario.getNetwork()).write("Z:/WinHome/otp-matsim/Portland/gtfs2matsim/network.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("Z:/WinHome/otp-matsim/Portland/gtfs2matsim/transit-schedule.xml");
		new VehicleWriterV1(((MutableScenario) scenario).getTransitVehicles()).writeFile("Z:/WinHome/otp-matsim/Portland/gtfs2matsim/transit-vehicles.xml");		
	}
	
	private static Scenario readScenario() {
		// GtfsConverter gtfs = new GtfsConverter("/Users/zilske/Documents/torino", new GeotoolsTransformation("WGS84", CRS));
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(CRS);
		config.controler().setLastIteration(0);
		config.scenario().setUseVehicles(true);
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		// GtfsConverter gtfs = new GtfsConverter("/Users/zilske/gtfs-bvg", scenario, new GeotoolsTransformation("WGS84", CRS));
		GtfsConverter gtfs = new GtfsConverter(GTFSFeed.fromFile("Z:/WinHome/otp-matsim/Portland/gtfs_unzipped"), scenario, TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, CRS));
		gtfs.setDate(LocalDate.of(2015, 2, 10));
		gtfs.convert();
		return scenario;
	}



}
