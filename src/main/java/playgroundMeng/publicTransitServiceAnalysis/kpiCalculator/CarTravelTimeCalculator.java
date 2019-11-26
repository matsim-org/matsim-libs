package playgroundMeng.publicTransitServiceAnalysis.kpiCalculator;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.Trip;
import playgroundMeng.publicTransitServiceAnalysis.infoCollector.EventsReader;
import playgroundMeng.publicTransitServiceAnalysis.others.PtAccessabilityConfig;

public class CarTravelTimeCalculator {
	private final Network network;
	private TravelTimeCalculator travelTimeCalculator;
	private LeastCostPathCalculator leastCostPathCalculator;

	private static CarTravelTimeCalculator caculator = null;

	private CarTravelTimeCalculator() {
		this.network = PtAccessabilityConfig.getInstance().getNetwork();
		this.travelTimeCalculator = EventsReader.getInstance().getTravelTimeCalculator();
		this.setLeastCostPathCaculator();

	}

	public static CarTravelTimeCalculator getInstance() {
		if (caculator == null)
			caculator = new CarTravelTimeCalculator();
		return caculator;
	}

	private void setLeastCostPathCaculator() {
		PreProcessDijkstra preProcessData = new PreProcessDijkstra();
		preProcessData.run(network);
		TravelTime timeFunction = travelTimeCalculator.getLinkTravelTimes();
		TravelDisutility costFunction = new OnlyTimeDependentTravelDisutility(timeFunction);

		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		leastCostPathCalculator = dijkstraFactory.createPathCalculator(network, costFunction, timeFunction);
	}

	public void caculate(Trip trip) {
		Person person = null;
		Vehicle vehicle = null;
		Node fromNode = trip.getActivityEndImp().getLink().getFromNode();
		Node toNode = trip.getActivityStartImp().getLink().getToNode();

		Path path = leastCostPathCalculator.calcLeastCostPath(fromNode, toNode, trip.getActivityEndImp().getTime(),
				person, vehicle);
		trip.setCarTravelInfo(new CarTravelInfo(path));

	}

	public TravelTimeCalculator getTravelTimeCalculator() {
		return travelTimeCalculator;
	}

	public class CarTravelInfo {
		LeastCostPathCalculator.Path carPath;
		double travelTime;

		public CarTravelInfo(LeastCostPathCalculator.Path carPath) {
			this.carPath = carPath;
			this.travelTime = carPath.travelTime;
		}

		public void setCarPath(LeastCostPathCalculator.Path carPath) {
			this.carPath = carPath;
		}

		public LeastCostPathCalculator.Path getCarPath() {
			return carPath;
		}

		public void setTravelTime(double travelTime) {
			this.travelTime = travelTime;
		}

		public double getTravelTime() {
			return travelTime;
		}

		public double getFreeSpeedTravelTime() {
			double time = 0;
			for (Link link : carPath.links) {
				double timeLink = link.getLength() / link.getFreespeed();
				time = time + timeLink;
			}
			return time;
		}

		@Override
		public String toString() {
			return "CarTravelInfo [carPath=" + linksIdString(carPath.links) + ", travelTime=" + travelTime + "]";
		}

		private String linksIdString(List<Link> links) {
			List<Id<Link>> linkIds = new LinkedList<Id<Link>>();
			for (Link link : links) {
				linkIds.add(link.getId());
			}
			return linkIds.toString();
		}

	}
}
