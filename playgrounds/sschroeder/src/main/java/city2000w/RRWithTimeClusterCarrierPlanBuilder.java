package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.TourBuilder;
import vrp.algorithms.ruinAndRecreate.constraints.TimeAndCapacityPickupsDeliveriesSequenceConstraint;
import vrp.basics.CrowFlyDistance;
import vrp.basics.MultipleDepotsInitialSolutionFactory;
import freight.GreedyShipmentAggregator;
import freight.TourScheduler;
import freight.vrp.RRPDTWSolver;

public class RRWithTimeClusterCarrierPlanBuilder {
	
	public static class TimeBin {
		double start;
		double end;
		public TimeBin(double start, double end) {
			super();
			this.start = start;
			this.end = end;
		}
		
		public boolean isWithin(double time){
			if(time >= start && time < end){
				return true;
			}
			return false;
		}
		
	}
	
	public static class Monday extends TimeBin{
		public Monday() {
			super(0.0, 24*3600);
		}
	}
	
	public static class Tuesday extends TimeBin{

		public Tuesday() {
			super(24*3600, 2*24*3600);
		}
	}
	
	public static class Wednesday extends TimeBin{

		public Wednesday() {
			super(2*24*3600, 3*24*3600);
			// TODO Auto-generated constructor stub
		}
		
	}
	
	public static class Thursday extends TimeBin {

		public Thursday() {
			super(3*24*3600, 4*24*3600);
		}
		
	}
	
	public static class Friday extends TimeBin {
		public Friday() {
			super(4*24*3600, 5*24*3600);
		}
	}
	
	public static class Saturday extends TimeBin {
		public Saturday() {
			super(5*24*3600, 6*24*3600);
		}
	}
	
	public static interface Clusters {
		public TimeCluster getCluster(double time);
		
		public TimeCluster getCluster(double start, double end);
		
		public Iterator<TimeCluster> iterator();
		
		public boolean judge(double start, double end);
	}
	
	public static class GeneralClusters implements Clusters {

		private Map<TimeBin,TimeCluster> clusters = new HashMap<TimeBin, TimeCluster>();
		
		public void addTimeBin(TimeBin timeBin){
			clusters.put(timeBin, new TimeCluster(timeBin));
		}
		
		@Override
		public TimeCluster getCluster(double time) {
			for(TimeBin bin : clusters.keySet()){
				if(bin.start <= time && bin.end > time){
					return clusters.get(bin);
				}
			}
			return null;
		}

		@Override
		public TimeCluster getCluster(double start, double end) {
			for(TimeBin bin : clusters.keySet()){
				if(bin.start <= start && bin.end > start && bin.start <= end && bin.end > end){
					return clusters.get(bin);
				}
			}
			return null;
		}

		@Override
		public Iterator<TimeCluster> iterator() {
			return clusters.values().iterator();
		}
		
		public boolean isInCluster(double start, double end){
			boolean startInCluster = false;
			boolean endInCluster = false;
			for(TimeBin timeBin : clusters.keySet()){
				if(timeBin.start < start && timeBin.end > start){
					startInCluster = true;
				}
				if(timeBin.start < end && timeBin.end > end){
					endInCluster = true;
				}
			}
			return startInCluster && endInCluster;
		}

		@Override
		public boolean judge(double start, double end) {
			boolean startInCluster = false;
			boolean endInCluster = false;
			for(TimeBin timeBin : clusters.keySet()){
				if(timeBin.start < start && timeBin.end > start){
					startInCluster = true;
				}
				if(timeBin.start < end && timeBin.end > end){
					endInCluster = true;
				}
			}
			return startInCluster && endInCluster;
			
		}
		
	}
	
	static class WeekClusters implements Clusters{
		Map<TimeBin,TimeCluster> clusters = new HashMap<TimeBin, TimeCluster>();

		public WeekClusters() {
			Monday monday = new Monday();
			clusters.put(monday, new TimeCluster(monday));
			Tuesday tuesday = new Tuesday();
			clusters.put(tuesday, new TimeCluster(tuesday));
			Wednesday wednesday = new Wednesday();
			clusters.put(wednesday, new TimeCluster(wednesday));
			Thursday thursday = new Thursday();
			clusters.put(thursday, new TimeCluster(thursday));
			Friday friday = new Friday();
			clusters.put(friday, new TimeCluster(friday));
//			Saturday sat = new Saturday();
//			clusters.put(sat, new TimeCluster(sat));
		}
		
		public TimeCluster getCluster(double time){
			for(TimeBin bin : clusters.keySet()){
				if(bin.start <= time && bin.end > time){
					return clusters.get(bin);
				}
			}
			return null;
		}
		
		public boolean isInCluster(double start, double end){
			boolean startInCluster = false;
			boolean endInCluster = false;
			for(TimeBin timeBin : clusters.keySet()){
				if(timeBin.start < start && timeBin.end > start){
					startInCluster = true;
				}
				if(timeBin.start < end && timeBin.end > end){
					endInCluster = true;
				}
			}
			return startInCluster && endInCluster;
		}

		@Override
		public TimeCluster getCluster(double start, double end) {
			for(TimeBin bin : clusters.keySet()){
				if(bin.start <= start && bin.end > start && bin.start <= end && bin.end > end){
					return clusters.get(bin);
				}
			}
			return null;
		}

		@Override
		public Iterator<TimeCluster> iterator() {
			return clusters.values().iterator();
		}

		@Override
		public boolean judge(double start, double end) {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	static class TimeCluster {
		TimeBin timeBin;
		
		public List<Contract> getContracts() {
			return contracts;
		}

		public TimeCluster(TimeBin timeBin) {
			super();
			this.timeBin = timeBin;
		}
		List<Contract> contracts = new ArrayList<Contract>();
	}
	
	private static Logger logger = Logger.getLogger(RRWithTimeClusterCarrierPlanBuilder.class);
	
	private Network network;
	
	private int vehicleIdCounter = 0;
	
	private Carrier carrier;
	
	private GreedyShipmentAggregator greedyShipmentAggregator;
	
	private TourScheduler tourScheduler;
	
	private double vorlaufTime = 10*3600.0;
	
	public void setTourScheduler(TourScheduler tourScheduler) {
		this.tourScheduler = tourScheduler;
	}

	public void setGreedyShipmentAggregator(
			GreedyShipmentAggregator greedyShipmentAggregator) {
		this.greedyShipmentAggregator = greedyShipmentAggregator;
	}

	public void setCarrier(Carrier carrier) {
		this.carrier = carrier;
	}

	private Clusters timeClusters = new WeekClusters();
	
	private ShipmentFilter shipmentFilter;
	
	public void setShipmentFilter(ShipmentFilter shipmentFilter) {
		this.shipmentFilter = shipmentFilter;
	}

	public void setWeek(){
		timeClusters = new WeekClusters();
	}
	
	public void setTimeClusters(Clusters clusters){
		timeClusters = clusters;
	}
	
	public RRWithTimeClusterCarrierPlanBuilder(Network network){
		this.network = network;
	}
	public CarrierPlan buildPlan(CarrierCapabilities carrierCapabilities, Collection<Contract> contracts) {
		logger.info("build plan");
		logger.info(contracts.size() + " number of contracts");
		if(contracts.isEmpty()){
			return getEmptyPlan(carrierCapabilities);
		}
		clusterContracts(contracts);
		Iterator<TimeCluster> clusterIter = timeClusters.iterator();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		Double planScore = null;
		int counter = 0;
		while(clusterIter.hasNext()){
//			counter++;
//			if(counter > 1){
//				break;
//			}
			TimeCluster cluster = clusterIter.next();
			Collection<Shipment> shipments = filterAndGetShipments(cluster.contracts);
			if(shipments.isEmpty()){
				continue;
			}
			int carrierVehicleCapacity = carrierCapabilities.getCarrierVehicles().iterator().next().getCapacity();
			Map<Shipment,Collection<Shipment>> aggregatedShipments = greedyShipmentAggregator.aggregateShipments(shipments);
			RRPDTWSolver rrSolver = new RRPDTWSolver(aggregatedShipments.keySet(), carrierCapabilities.getCarrierVehicles(), network);
			rrSolver.setIniSolutionFactory(new MultipleDepotsInitialSolutionFactory());
			rrSolver.setnOfWarmupIterations(4);
			rrSolver.setnOfIterations(32);
			CrowFlyDistance crowFlyDistance = new CrowFlyDistance();
			crowFlyDistance.speed = 18;
			crowFlyDistance.detourFactor = 1.2;
			rrSolver.setCosts(crowFlyDistance);
			rrSolver.setConstraints(new TimeAndCapacityPickupsDeliveriesSequenceConstraint(carrierVehicleCapacity,vorlaufTime,crowFlyDistance));
//			RRSolver rrSolver = new RRSolver(shipments, carrierCapabilities.getCarrierVehicles(), network);
//			rrSolver.setIniSolutionFactory(new MultipleDepotsInitialSolutionFactory());
//			rrSolver.setnOfWarmupIterations(0);
//			rrSolver.setnOfIterations(2);
			Collection<Tour> tours = rrSolver.solve();
			Collection<ScheduledTour> myScheduledTours = tourScheduler.getScheduledTours(tours, aggregatedShipments);
//			Collection<ScheduledTour> myScheduledTours = makeScheduledTours(tours,cluster,carrierCapabilities);
			scheduledTours.addAll(myScheduledTours);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		carrierPlan.setScore(planScore);
		return carrierPlan;
	}
		

	public void setVorlaufTime(double vorlaufTime) {
		this.vorlaufTime = vorlaufTime;
	}

	private CarrierVehicle createVehicle(CarrierCapabilities carrierCapabilities) {
		CarrierVehicle vehicle = carrierCapabilities.getCarrierVehicles().iterator().next();
		Id vehicleId = makeVehicleId(vehicle.getVehicleId());
		return new CarrierVehicle(vehicleId, vehicle.getLocation());
	}

	private Id makeVehicleId(Id id) {
		vehicleIdCounter++;
		Id vId = new IdImpl(id.toString() + "_" + vehicleIdCounter);
		return vId;
	}

	private Collection<Shipment> filterAndGetShipments(List<Contract> contracts) {
		Collection<Shipment> shipments = new ArrayList<Shipment>();
		for(Contract c : contracts){
			if(shipmentFilter.judge(c.getShipment())){
				shipments.add(c.getShipment());
			}
		}
		return shipments;
	}

	private void clusterContracts(Collection<Contract> contracts) {
		for(Contract c : contracts){
			TimeCluster timeCluster = timeClusters.getCluster(c.getShipment().getPickupTimeWindow().getStart());
			if(timeCluster != null){
				timeCluster.contracts.add(c);
			}
			else{
				logger.warn("contract out of week. could not consider contract " + c.getShipment());
			}
		}
	}

	private CarrierPlan getEmptyPlan(CarrierCapabilities carrierCapabilities) {
		Collection<Tour> tours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		for(CarrierVehicle cv : carrierCapabilities.getCarrierVehicles()){
			TourBuilder tourBuilder = new TourBuilder();
			Id vehicleStartLocation = cv.getLocation();
			tourBuilder.scheduleStart(vehicleStartLocation);
			tourBuilder.scheduleEnd(vehicleStartLocation);
			Tour tour = tourBuilder.build();
			tours.add(tour);
			ScheduledTour scheduledTour = new ScheduledTour(tour, cv, 0.0);
			scheduledTours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		return carrierPlan;
	}
}
