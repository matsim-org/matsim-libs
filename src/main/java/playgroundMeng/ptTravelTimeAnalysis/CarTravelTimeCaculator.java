package playgroundMeng.ptTravelTimeAnalysis;



import java.util.LinkedList;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator.Builder;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.Trip;

public class CarTravelTimeCaculator {
	
	Network network;
	TravelTimeConfig travelTimeConfig;
	
	Trip trip;
	TravelTimeCalculator ttcCalculator;
	
	@Inject
	public CarTravelTimeCaculator(Trip trip, Network network, TravelTimeConfig timeConfig) {
		LinkedList<Link> ptLinks = new LinkedList<Link>();
		for(Link link: network.getLinks().values()) {
			if(link.getId().toString().contains("pt")) {
				ptLinks.add(link);
			}
		}
		for(Link link: ptLinks) {
			network.removeLink(link.getId());
		}
		new NetworkCleaner().run(network);
		this.network = network;
		this.travelTimeConfig = timeConfig;
		this.trip = trip;
		
	}
	public void caculate() {
		this.travelTimeCalculatorBuild();
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(network);
		TravelTime timeFunction = ttcCalculator.getLinkTravelTimes();
		TravelDisutility costFunction = new OnlyTimeDependentTravelDisutility(timeFunction) ;
		  
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		LeastCostPathCalculator leastCostPathCalculator = dijkstraFactory.createPathCalculator(network, costFunction, timeFunction);
		 
		Person person = null;
		Vehicle vehicle = null;
		
		Node fromNode = NetworkUtils.getNearestNode(network, trip.getActivityEndImp().getCoord());
		Node toNode = NetworkUtils.getNearestNode(network, trip.getActivityStartImp().getCoord());
		
		   
		Path path = leastCostPathCalculator.calcLeastCostPath(fromNode, toNode, trip.getActivityEndImp().getStartTime(), person, vehicle);
		this.trip.setCarTravelInfo(new CarTravelInfo(path));

	}
	private void travelTimeCalculatorBuild() {
		Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setMaxTime(this.travelTimeConfig.getEndTime());
		builder.setTimeslice(this.travelTimeConfig.getTimeSlice());
		ttcCalculator = builder.build();
	}
	

}
