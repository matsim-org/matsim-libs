package playground.sergioo.routingAnalysisCEPAS;

import java.sql.Time;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.sergioo.routingAnalysisCEPAS.MainRoutes.Journey;
import playground.sergioo.routingAnalysisCEPAS.MainRoutes.Trip;

public class RoutesPopulation implements PersonAlgorithm {
	
	private Map<Journey, Integer> journeysPlan = new HashMap<Journey, Integer>();
	private Scenario scenario;
	private final String prevKey;
	
	public RoutesPopulation(Scenario scenario, String prevKey) {
		this.prevKey = prevKey;
		this.scenario = scenario;
	}

	@Override
	public void run(Person person) {
		Journey journey = null;
		for(PlanElement planElement:person.getSelectedPlan().getPlanElements())
			if(planElement instanceof Activity) {
				if(!((Activity)planElement).getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
					if(journey!=null && journey.trips.size()>0) {
						String key1 = journey.trips.peek().origin.toString(), key2 = MainRoutes.getLast(journey.trips).destination.toString() ;
						if(prevKey.equals(key1+"-"+key2))
							if(!MainRoutes.repeated(journey, key1, key2) && MainRoutes.goodDistance(journey)) {
								Journey journey2 = MainRoutes.differentJourney(journeysPlan, journey); 
								if(journey2 == null)
									journeysPlan.put(journey, 1);
								else
									journeysPlan.put(journey2,journeysPlan.get(journey2)+1);
							}
					}
					journey = new Journey();
				}
			}
			else if(((Leg)planElement).getMode().equals("pt")) {
				String[] parts = (((Leg)planElement).getRoute()).getRouteDescription().split("===");
				Coord startCoord = MainRoutes.transformation.transform(scenario.getTransitSchedule().getFacilities().get(Id.create(parts[1], TransitStopFacility.class)).getCoord());
				Coord endCoord = MainRoutes.transformation.transform(scenario.getTransitSchedule().getFacilities().get(Id.create(parts[4], TransitStopFacility.class)).getCoord());
				journey.trips.add(new Trip((float)((Leg)planElement).getTravelTime(), MainRoutes.getDistance(scenario.getNetwork(), scenario.getTransitSchedule(), parts), parts[1], parts[4], new Time((long)(((Leg)planElement).getDepartureTime()*1000)), startCoord.getY(), startCoord.getX(), endCoord.getY(), endCoord.getX()));
			}
	}

	public Map<Journey, Integer> getJourneyPlan() {
		return journeysPlan;
	}
	
}
