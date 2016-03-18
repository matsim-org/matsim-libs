package playground.jbischoff.taxibus.scenario.network;

import com.conveyal.gtfs.GTFSFeed;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import playground.andreas.mzilske.bvg09.MergeNetworks;

import java.time.LocalDate;

public class WolfsburgGTFSConvert {

	public static void main( String[] args ) {
//		CoordinateTransformation transform0  = new WGS84toCH1903LV03() ;
		// ---
		CoordinateTransformation ct = 
				  TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:25832");
		Config config = ConfigUtils.createConfig();
		Scenario scenario0 = ScenarioUtils.createScenario(config);
		// ---
		final String filepath0 = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/pt/";
		// ---
		GtfsConverter converter = new GtfsConverter(GTFSFeed.fromFile(filepath0), scenario0, ct );
		converter.setDate(LocalDate.of(2015, 10, 8));
		converter.convert();
		// ---
		new NetworkWriter(scenario0.getNetwork()).write( filepath0 + "/output_network.xml.gz");
		Scenario scenario1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		
		
		new MatsimNetworkReader(scenario1.getNetwork()).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/network.xml");
		
		MergeNetworks.merge(scenario1.getNetwork(), "pt",scenario0.getNetwork());
		
		new NetworkWriter(scenario1.getNetwork()).write( filepath0 + "/networkpt.xml.gz");
		
		
		new TransitScheduleWriter( scenario0.getTransitSchedule() ).writeFile( filepath0 + "/output_transitschedule.xml.gz");
		new VehicleWriterV1( scenario0.getTransitVehicles() ).writeFile( filepath0 + "/output_transitvehicles.xml.gz");
	}

}