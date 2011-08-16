package playground.fhuelsmann.emission.analysis.mobility;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class CalcAvgTripDurations implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	//TO DO: getRelevantPopulation between ZoneId x and all other zones, 
	//calculate the aggregated travel time for all people traveling between zone x and all other zones 
	//and divide it by the number of people traveling between zone x and all other zones 

	/**
	 * stores the last known departure time per agent
	 */
	private final Map<String, Double> agentDepartures = new TreeMap<String, Double>();
	

	private double travelTimeSum;
	private int travelTimeCnt;

	public void handleEvent(final AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId().toString(), event.getTime());
	}

	public void handleEvent(final AgentArrivalEvent event) {
		double departureTime = this.agentDepartures.get(event.getPersonId().toString());
		double travelTime = event.getTime() - departureTime;
		this.travelTimeSum+= travelTime;
		this.travelTimeCnt++;
		
	}

	public void reset(final int iteration) {
		this.agentDepartures.clear();
	
	}

	/**
	 * @return average trip duration from zone x to all other zones
	 */
	public Map<Id, Double> getAvgTripDuration(Population population) {
		Map<Id, Double> avgTripduration = new TreeMap<Id, Double>();
		Id zoneId = null;
		
		int count = this.travelTimeCnt;
		if (count == 0) {
			avgTripduration.put(zoneId, 0.0);
			return avgTripduration;
			
		}
		// else
		System.out.println("+++++++++++++++++++++averageTravelTime"+this.travelTimeSum/count);
		
		avgTripduration.put(zoneId, travelTimeSum/travelTimeCnt);
		return avgTripduration;
		
	}
}