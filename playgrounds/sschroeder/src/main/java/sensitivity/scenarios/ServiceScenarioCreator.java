package sensitivity.scenarios;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.solution.route.activity.TimeWindow;
import jsprit.core.util.Coordinate;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;



public class ServiceScenarioCreator {
	
	static class BoundingGeoByLink {
		private Id minLink;
		
		private Id maxLink;

		public BoundingGeoByLink(Id minLink, Id maxLink) {
			super();
			this.minLink = minLink;
			this.maxLink = maxLink;
		}

		/**
		 * @return the minLink
		 */
		public Id getMinLink() {
			return minLink;
		}

		/**
		 * @return the maxLink
		 */
		public Id getMaxLink() {
			return maxLink;
		}
		
		
	}
	
	static class BoundingGeoByCoord {
		
		private double minX;
		private double minY;
		private double maxX;
		private double maxY;
		
		public BoundingGeoByCoord(double minX, double minY, double maxX, double maxY) {
			super();
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
		}
		/**
		 * @return the minX
		 */
		public double getMinX() {
			return minX;
		}
		/**
		 * @return the minY
		 */
		public double getMinY() {
			return minY;
		}
		/**
		 * @return the maxX
		 */
		public double getMaxX() {
			return maxX;
		}
		/**
		 * @return the maxY
		 */
		public double getMaxY() {
			return maxY;
		}
		
		
		
		
	}
	
	static class BoundingDemand {
		private int minDemand;
		private int maxDemand;
		public BoundingDemand(int minDemand, int maxDemand) {
			super();
			this.minDemand = minDemand;
			this.maxDemand = maxDemand;
		}
		/**
		 * @return the minDemand
		 */
		public int getMinDemand() {
			return minDemand;
		}
		/**
		 * @return the maxDemand
		 */
		public int getMaxDemand() {
			return maxDemand;
		}
		
		
	}
	
	static class BoundingOperationStart {
		
		private double minStart;
		
		private double maxStart;

		public BoundingOperationStart(double minStart, double maxStart) {
			super();
			this.minStart = minStart;
			this.maxStart = maxStart;
		}

		/**
		 * @return the minStart
		 */
		public double getMinStart() {
			return minStart;
		}

		/**
		 * @return the maxStart
		 */
		public double getMaxStart() {
			return maxStart;
		}
		
	}
	
	static class SchedulingHorizon {
		private double earliestDep;
		
		private double latestArr;

		public SchedulingHorizon(double earliestDep, double latestArr) {
			super();
			this.earliestDep = earliestDep;
			this.latestArr = latestArr;
		}

		/**
		 * @return the earliestDep
		 */
		public double getEarliestDep() {
			return earliestDep;
		}

		/**
		 * @return the latestArr
		 */
		public double getLatestArr() {
			return latestArr;
		}
		
		
	}
	
	public static void main(String[] args) {
		int nCustomer = 100;
		Random random = new Random(Long.MAX_VALUE);
		
	}
	
	private final Network network;
	
	private final int nCustomers;
	
	private final BoundingGeoByCoord boundingGeo;
	
	private final BoundingGeoByLink boundGeoByLink;
	
	private Random random = new Random(Long.MAX_VALUE);
	
	private BoundingDemand boundingDemand = new BoundingDemand(1,1);
	
	private BoundingOperationStart boundingOperationStart = new BoundingOperationStart(0.0, 24*3600);
	
	private List<Double> timeWindowLengths = null;
	
	private List<Integer> discreteDemands = null;
	
	private double serviceTime = 0;

	private double minEarliest;

	private double maxEarliest;

	private List<Double> startTimes;

	public ServiceScenarioCreator(Network network, BoundingGeoByCoord boundingGeo, int nServices) {
		this.network = network;
		this.boundingGeo = boundingGeo;
		this.nCustomers = nServices;
		this.boundGeoByLink = null;
	}
	
	public ServiceScenarioCreator(Network network, BoundingGeoByLink boundingGeoByLink, int nServices) {
		this.network = network;
		this.boundingGeo = null;
		this.nCustomers = nServices;
		this.boundGeoByLink = boundingGeoByLink;
	}
	
	/**
	 * @param random the random to set
	 */
	public ServiceScenarioCreator setRandom(Random random) {
		this.random = random;
		return this;
	}

	

	/**
	 * @param boundingDemand the boundingDemand to set
	 */
	public ServiceScenarioCreator setBoundingDemand(BoundingDemand boundingDemand) {
		this.boundingDemand = boundingDemand;
		return this;
	}

	/**
	 * @param boundingOperationStart the boundingOperationStart to set
	 */
	public ServiceScenarioCreator setBoundingOperationStart(BoundingOperationStart boundingOperationStart) {
		this.boundingOperationStart = boundingOperationStart;
		return this;
	}

	/**
	 * @param timeWindowLengths the timeWindowLengths to set
	 */
	public ServiceScenarioCreator setTimeWindowLengths(List<Double> lengthsInSeconds, double minEarliestStartTimeInSeconds, double maxEarliestStartTimeInSeconds) {
		this.timeWindowLengths = lengthsInSeconds;
		this.minEarliest = minEarliestStartTimeInSeconds;
		this.maxEarliest = maxEarliestStartTimeInSeconds;
		return this;
	}
	
	public ServiceScenarioCreator setTimeWindowLengths(List<Double> lengthsInSeconds, List<Double> startTimesInSeconds) {
		this.timeWindowLengths = lengthsInSeconds;
		this.startTimes = startTimesInSeconds;
		return this;
	}

	/**
	 * @param discreteDemands the discreteDemands to set
	 */
	public ServiceScenarioCreator setDiscreteDemands(List<Integer> discreteDemands) {
		this.discreteDemands = discreteDemands;
		return this;
	}

	/**
	 * @param serviceTime the serviceTime to set
	 */
	public ServiceScenarioCreator setServiceTime(double inSeconds) {
		this.serviceTime = inSeconds;
		return this;
	}

	
	public void createAndLoad(VehicleRoutingProblem.Builder builder){
		List<Link> links = getLinks(); 
		for(int i=0;i<nCustomers;i++){
			int demand = getDemand();
			Service.Builder serviceBuilder = Service.Builder.newInstance(""+(i+1)).addSizeDimension(0, demand);
			Link homeLink = getLink(links);
			serviceBuilder.setCoord(Coordinate.newInstance(homeLink.getCoord().getX(),homeLink.getCoord().getY()));
			serviceBuilder.setLocationId(homeLink.getId().toString());
			serviceBuilder.setServiceTime(serviceTime);
			if(timeWindowLengths != null){
				serviceBuilder.setTimeWindow(getTW());
			}
			Service service = serviceBuilder.build();
			builder.addJob(service);
		}
	}

	private TimeWindow getTW() {
		int randomTWLengthIndex = random.nextInt(timeWindowLengths.size());
		double twLength = timeWindowLengths.get(randomTWLengthIndex);
		double earliestStartTime;
		if(startTimes != null){
			int randomIndex = random.nextInt(startTimes.size());
			earliestStartTime = startTimes.get(randomIndex);
		}
		else{
			earliestStartTime = Math.round(minEarliest + (maxEarliest - minEarliest)*random.nextDouble());
		}
		TimeWindow tw = TimeWindow.newInstance(earliestStartTime, earliestStartTime + twLength);
		return tw;
	}

	private Link getLink(List<Link> links) {
		int randomIndex = random.nextInt(links.size());
		return links.get(randomIndex);
	}

	private int getDemand() {
		int randomDemand = boundingDemand.getMinDemand() + (int)((boundingDemand.getMaxDemand()-boundingDemand.getMinDemand())*random.nextDouble());
		return randomDemand;
	}

	private List<Link> getLinks() {
		BoundingGeoByCoord geoByCoord;
		if(boundGeoByLink != null){
			Link minLink = network.getLinks().get(boundGeoByLink.getMinLink());
			Link maxLink = network.getLinks().get(boundGeoByLink.getMaxLink());
			geoByCoord = new BoundingGeoByCoord(minLink.getCoord().getX(), minLink.getCoord().getY(), 
					maxLink.getCoord().getX(), maxLink.getCoord().getY());
		}
		else{
			geoByCoord = this.boundingGeo;
		}
		List<Link> links = new ArrayList<Link>();
		for(Link l : network.getLinks().values()){
			Coord coord = l.getCoord();
			if(inBoundingBox(coord,geoByCoord)){
				links.add(l);
			}
		}
		return links;
	}

	private boolean inBoundingBox(Coord coord, BoundingGeoByCoord geoByCoord) {
		if(coord.getX() >= geoByCoord.getMinX() && coord.getY() >= geoByCoord.getMinY()){
			if(coord.getX() <= geoByCoord.getMaxX() && coord.getY() <= geoByCoord.getMaxY()){
				return true;
			}
		}
		return false;
	}
	

}
