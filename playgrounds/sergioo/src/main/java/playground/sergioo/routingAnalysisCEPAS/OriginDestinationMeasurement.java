package playground.sergioo.routingAnalysisCEPAS;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;
import playground.sergioo.routingAnalysisCEPAS.MainRoutes.Journey;
import playground.sergioo.routingAnalysisCEPAS.MainRoutes.Trip;

public class OriginDestinationMeasurement {
	
	private Id<ActivityFacility> origin;
	private Id<ActivityFacility> destination;
	private Map<String, Journey> allJourneys = new HashMap<String, Journey>();

	public OriginDestinationMeasurement(Id<ActivityFacility> origin, Id<ActivityFacility> destination) {
		this.origin = origin;
		this.destination = destination;
	}
	
	public void processOriginDestination(DataBaseAdmin dba) throws SQLException, NoConnectionException {
		
	}
	
	private Trip getLast(PriorityQueue<Trip> trips) {
		Iterator<Trip> it = trips.iterator();
		Trip res = null;
		while(it.hasNext())
			res = it.next();
		return res;
	}

	public int getNumJourneys() {
		return allJourneys.size();
	}

	public int count(Map<String, Journey> allJourneys) {
		for(Entry<String, Journey> journey:allJourneys.entrySet())
			if(journey.getValue().trips.peek().origin.equals(origin) && getLast(journey.getValue().trips).destination.equals(destination))
				this.allJourneys.put(journey.getKey(), journey.getValue());
		return this.allJourneys.size();
	}
}
