package playground.gleich.otp_matsim.ulm;


import com.conveyal.gtfs.GTFSFeed;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.network.NetworkWriter;
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
	

	static final String CRS = "EPSG:3857";
	private static Population population;
	
	public static void main(String[] args) {
		new Convert().convert();
	}

	private void convert() {
		final Scenario scenario = readScenario();
		System.out.println("Scenario has " + scenario.getNetwork().getLinks().size() + " links.");
		scenario.getConfig().controler().setMobsim("qsim");
		scenario.getConfig().qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		scenario.getConfig().qsim().setSnapshotPeriod(1);
		scenario.getConfig().qsim().setRemoveStuckVehicles(false);
		ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setColoringScheme(ColoringScheme.gtfs);
		ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawTransitFacilities(false);
		scenario.getConfig().transitRouter().setMaxBeelineWalkConnectionDistance(1.0);

		new NetworkWriter(scenario.getNetwork()).write("Z:/WinHome/otp-matsim/Ulm/gtfs2matsim/network.xml");
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("Z:/WinHome/otp-matsim/Ulm/gtfs2matsim/transit-schedule.xml");
		// getTransitVehicles() instead of getVehicles()
		new VehicleWriterV1(((MutableScenario) scenario).getTransitVehicles()).writeFile("Z:/WinHome/otp-matsim/Ulm/gtfs2matsim/transit-vehicles.xml");		
	}
	
	private static Scenario readScenario() {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(CRS);
		config.controler().setLastIteration(0);
		config.scenario().setUseVehicles(true);
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		GtfsConverter gtfs = new GtfsConverter(GTFSFeed.fromFile("Z:/WinHome/otp-matsim/Ulm/Original"), scenario, TransformationFactory.getCoordinateTransformation(
				TransformationFactory.WGS84, CRS));
		gtfs.setDate(LocalDate.of(2014,2,10));
		gtfs.convert();
		return scenario;
	}

}
