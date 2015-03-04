package playground.artemc.hits;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import playground.artemc.utils.NoConnectionException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Using MATSim network and transit schedule, returns ArrayList of stops for a particular bus line. In case there a different lengths/variations of a particular bus line (e.g. last services ends at an earlier stop), the line with maximum number of stops is returned.
 *
 * @author artemc
 */

public class AssignStopsToHITS {
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	public  AssignStopsToHITS(){
		//new NetworkReaderMatsimV1(scenario).parse("C:/Work/MATSim/singapore7.xml");
		scenario.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(scenario).readFile("C:/Work/MATSim/transitScheduleWAM.xml");		
	}
	
	ArrayList<String> findStopIDsBus(String Busline) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, NoConnectionException{
		String StopID = null;
		
		Id<TransitLine> bline = Id.create(Busline,TransitLine.class);
		ArrayList<Id> routeIds = new ArrayList<Id>();
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		ArrayList<String> stopList = new ArrayList<String>();

		Set<Id<TransitRoute>> routes = scenario.getTransitSchedule().getTransitLines().get(bline).getRoutes().keySet();
		for(Id currentRouteID:routes){
			routeIds.add(currentRouteID);
		}
		
		//Choose the route with most stops
		int routeSize=0;
		for(Id routeID:routeIds){
			List<TransitRouteStop> stops_temp = scenario.getTransitSchedule().getTransitLines().get(bline).getRoutes().get(routeID).getStops();
			if(stops_temp.size()>routeSize){
				routeSize=stops_temp.size();
				stops = stops_temp; 
			}
		}
		
		//Choose transit stop
		for(TransitRouteStop stopId:stops){	
			stopList.add(stopId.getStopFacility().getId().toString());		
		}
		
		return stopList;
	}

}
