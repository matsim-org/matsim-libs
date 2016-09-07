package playground.dziemke.accessibility.ptmatrix;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.dziemke.utils.LogToOutputSaver;

/**
 * @author dziemke
 */
public class MatrixBasedPtInputUtils {
	private static final Logger log = Logger.getLogger(MatrixBasedPtInputUtils.class);

	public static void main(String[] args) {
		String transitScheduleFile = "../../matsim/examples/pt-tutorial/transitschedule.xml";
		String networkFile = "../../matsim/examples/pt-tutorial/multimodalnetwork.xml";
//		String outputRoot = "";
		String outputRoot = "/Users/dominik/test/";
		LogToOutputSaver.setOutputDirectory(outputRoot);
		
		double departureTime = 8. * 60 * 60;

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		
		TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenario);
		transitScheduleReader.readFile(transitScheduleFile);
		
//		Map<Id<Facility<?>>, Facility<?>> ptMatrixLocationsMap = new HashMap<>();
		
//		for (TransitStopFacility transitStopFacility: scenario.getTransitSchedule().getFacilities().values()) {
//			Id<Coord> id = Id.create(transitStopFacility.getId(), Coord.class);
//			Coord coord = transitStopFacility.getCoord();
//			ptMatrixLocationsMap.put(id, transitStopFaciilty);
//		}
				
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFile);

		createStopsFile(scenario.getTransitSchedule().getFacilities(), outputRoot + "ptStops.csv", ",");
		
		// The locationFacilitiesMap is passed twice: Once for origins and once for destinations.
		// In other uses the two maps may be different -- thus the duplication here.
		new ThreadedMatrixCreator(scenario, scenario.getTransitSchedule().getFacilities(), scenario.getTransitSchedule().getFacilities(), departureTime, outputRoot, " ", 1);		
	}
	
	
	/**
	 * Creates a csv file containing the public transport stops or measure points
	 */
	public static void createStopsFile(Map<? extends Id,? extends Facility> locationFacilitiesMap, String outputFileStops, String separator) {
		final CSVFileWriter stopsWriter = new CSVFileWriter(outputFileStops, separator);
		
		stopsWriter.writeField("id");
		stopsWriter.writeField("x");
		stopsWriter.writeField("y");
		stopsWriter.writeNewLine();

		for (Facility fac : locationFacilitiesMap.values() ) {
			stopsWriter.writeField(fac.getId());
			stopsWriter.writeField(fac.getCoord().getX());
			stopsWriter.writeField(fac.getCoord().getY());
			stopsWriter.writeNewLine();
		}
		
		stopsWriter.close();
		log.info("Stops file based on schedule written.");
	}
}