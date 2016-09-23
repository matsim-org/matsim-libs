package contrib.baseline.build;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV1;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.algorithms.WorldConnectLocations;

public class TestRoutes {
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		config.setParam("f2l", "inputF2LFile", "/home/sebastian/build_switzerland/facilitiesLinks.f2l");
		
		new PopulationReader(scenario).readFile("/home/sebastian/build_switzerland/population.xml.gz");
		new FacilitiesReaderMatsimV1(scenario).readFile("/home/sebastian/build_switzerland/facilities.xml.gz");
		new NetworkReaderMatsimV1(scenario.getNetwork()).readFile("/home/sebastian/build_switzerland/mmNetwork.xml.gz");
		
        WorldConnectLocations wcl = new WorldConnectLocations(config);
        wcl.connectFacilitiesWithLinks(scenario.getActivityFacilities(), scenario.getNetwork());
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);
		
		PreProcessDijkstra preprocessDijkstra = new PreProcessDijkstra();
		preprocessDijkstra.run(scenario.getNetwork());
		
		LeastCostPathCalculator leastCostPathCalculator = new Dijkstra(scenario.getNetwork(), travelDisutility, travelTime, preprocessDijkstra);
		RoutingModule routingModule = new NetworkRoutingModule("car", scenario.getPopulation().getFactory(), scenario.getNetwork(), leastCostPathCalculator);
		
		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan(), routingModule.getStageActivityTypes());
			
			for (Trip trip : trips) {
				if (mainModeIdentifier.identifyMainMode(trip.getTripElements()).equals("car")) {
					ActivityFacility origin = scenario.getActivityFacilities().getFacilities().get(trip.getOriginActivity().getFacilityId());
					ActivityFacility destination = scenario.getActivityFacilities().getFacilities().get(trip.getOriginActivity().getFacilityId());
				
					List<Leg> legs = trip.getLegsOnly();
					if (legs.size() > 1) throw new IllegalStateException();
					
					List<? extends PlanElement> result = routingModule.calcRoute(origin, destination, legs.get(0).getDepartureTime(), person);
					legs.get(0).setRoute(((Leg)result.get(0)).getRoute());
				}
			}
		}
	}
}
