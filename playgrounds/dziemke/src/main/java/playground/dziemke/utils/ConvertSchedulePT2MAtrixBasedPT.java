package playground.dziemke.utils;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


// maybe look at "TransitStopsToCSV" by Sergio

public class ConvertSchedulePT2MAtrixBasedPT {

	public static void main(String[] args) {
		String transitScheduleFile = "/Users/dominik/Workspace/runs-svn/nmbm_minibuses/nmbm/output/jtlu14i/jtlu14i.0.transitSchedule_test.xml";
		
		String outputFileName = "/Users/dominik/Workspace/data/nmbm/transitSchedule/minibusTransitSchedule.csv";
		
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		
		reader.readFile(transitScheduleFile);
		
		Map<Id<TransitStopFacility>, TransitStopFacility> transitStopFacilitiesMap = scenario.getTransitSchedule().getFacilities();
		
	
		final MatrixBasedCSVWriter writer = new MatrixBasedCSVWriter(outputFileName);
		
		int i = 1;
			
//		for (Id<TransitStopFacility> transitStopFacilityId : transitStopFacilitiesMap.keySet()) {
//			TransitStopFacility transitStopFacility = transitStopFacilitiesMap.get(transitStopFacilityId);
			
		for (TransitStopFacility transitStopFacility : transitStopFacilitiesMap.values()) {
			double xCoord = transitStopFacility.getCoord().getX();
			double yCoord = transitStopFacility.getCoord().getY();
			
			writer.writeField(i);
			writer.writeField(xCoord);
			writer.writeField(yCoord);
			writer.writeNewLine();
			i++;
		}
		
		writer.close();

	}

}
