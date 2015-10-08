package playground.benjamin.scenarios.santiago.gtfs;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import playground.mzilske.gtfs.GtfsConverter;

public class SantiagoGtfsConvert {

	public static void main( String[] args ) {
		CoordinateTransformation transform0  = new WGS84toCH1903LV03() ;
		// ---
		Config config = ConfigUtils.createConfig();
		Scenario scenario0 = ScenarioUtils.createScenario(config);
		// ---
		final String filepath0 = "/Users/nagel/shared-svn/studies/countries/cl/santiago_pt_demand_matrix/gtfs_201306";
		// ---
		GtfsConverter converter = new GtfsConverter(filepath0, scenario0, transform0 ) ;
		converter.convert() ;
		// ---
		new NetworkWriter(scenario0.getNetwork()).write( filepath0 + "/output_network.xml.gz");
		new TransitScheduleWriter( scenario0.getTransitSchedule() ).writeFile( filepath0 + "/output_transitschedule.xml.gz");
		new VehicleWriterV1( scenario0.getTransitVehicles() ).writeFile( filepath0 + "/output_transitvehicles.xml.gz");
	}

}
