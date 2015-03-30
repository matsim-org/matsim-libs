package playground.dziemke.utils;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


// maybe look at "TransitStopsToCSV" by Sergio

public class ConvertSchedulePT2MAtrixBasedPT {

	public static void main(String[] args) {
		String transitScheduleFile = "/Users/dominik/Workspace/runs-svn/nmbm_minibuses/nmbm/output/jtlu14i/jtlu14i.0.transitSchedule.xml.gz";
		String configFile = "/Users/dominik/Workspace/runs-svn/nmbm_minibuses/nmbm/configJTLU_14i.xml";
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		double departureTime = 8. * 60 * 60;
		
		Id<Person> id = Id.createPersonId(1);
		
		Person person = PopulationUtils.createPopulation(config).getFactory().createPerson(id);
		
		String outputFileName = "/Users/dominik/Workspace/data/nmbm/transit/minibusStops.csv";
		
//		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Scenario scenario = ScenarioUtils.loadScenario(config);

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
		
		//################################################################################
		
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(), 
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
				scenario.getConfig().vspExperimental());
		
		// TransitRouterImpl(final TransitRouterConfig config, final TransitSchedule schedule)
		TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);
		
		final MatrixBasedCSVWriter writer2 = new MatrixBasedCSVWriter(outputFileName);
		
		int m = 0;
		
		for (TransitStopFacility transitStopFacilityFrom : transitStopFacilitiesMap.values()) {
			for (TransitStopFacility transitStopFacilityTo : transitStopFacilitiesMap.values()) {
				Coord fromCoord = transitStopFacilityFrom.getCoord();
				Coord toCoord = transitStopFacilityTo.getCoord();
				
				List<Leg> legList = transitRouter.calcRoute(fromCoord, toCoord, departureTime, person);
				
				double travelTime = 0.;
				
				for(int j=0; j < legList.size(); j++) {
					Leg leg = legList.get(j);
					travelTime = travelTime + leg.getTravelTime();
					
				}
				
//				writer2.writeField(m);
//				writer2.writeField(xCoord);
//				writer2.writeField(yCoord);
//				writer2.writeNewLine();
				
			}
		}
	

	}

}
