package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierContract;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.Tour.Delivery;
import playground.mzilske.freight.Tour.Pickup;
import playground.mzilske.freight.Tour.TourElement;
import playground.mzilske.freight.TourBuilder;
import vrp.api.Customer;
import vrp.basics.TourActivity;
import freight.vrp.LocationsImpl;
import freight.vrp.RuinAndRecreateSolver;
import freight.vrp.VRPTransformation;

public class RAndRPickupAndDeliveryCarrierPlanBuilder {
	
	static class TimeBin {
		double start;
		double end;
		public TimeBin(double start, double end) {
			super();
			this.start = start;
			this.end = end;
		}
		
	}
	
	static class Monday extends TimeBin{
		public Monday() {
			super(0.0, 24*3600);
		}
	}
	
	static class Tuesday extends TimeBin{

		public Tuesday() {
			super(24*3600, 2*24*3600);
		}
	}
	
	static class Wednesday extends TimeBin{

		public Wednesday() {
			super(2*24*3600, 3*24*3600);
			// TODO Auto-generated constructor stub
		}
		
	}
	
	static class Thursday extends TimeBin {

		public Thursday() {
			super(3*24*3600, 4*24*3600);
		}
		
	}
	
	static class Friday extends TimeBin {

		public Friday() {
			super(4*24*3600, 5*24*3600);
		}
	}
	
	static interface Clusters {
		public TimeCluster getCluster(double time);
		
		public TimeCluster getCluster(double start, double end);
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
		}
		
		public TimeCluster getCluster(double time){
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
	}
	
	static class TimeCluster {
		TimeBin timeBin;
		
		public List<CarrierContract> getContracts() {
			return contracts;
		}

		public TimeCluster(TimeBin timeBin) {
			super();
			this.timeBin = timeBin;
		}
		List<CarrierContract> contracts = new ArrayList<CarrierContract>();
	}
	
	private static Logger logger = Logger.getLogger(RAndRPickupAndDeliveryCarrierPlanBuilder.class);
	
	private Network network;
	
	private VRPTransformation vrpTrafo;
	
	private Clusters timeClusters = null;
	
	public void setWeek(){
		timeClusters = new WeekClusters();
	}
	
	public void setTimeClusters(Clusters clusters){
		timeClusters = clusters;
	}

	public RAndRPickupAndDeliveryCarrierPlanBuilder(Network network){
		this.network = network;
		iniTrafo();
	}

	private void iniTrafo() {
		LocationsImpl locations = new LocationsImpl();
		makeLocations(locations);
		vrpTrafo = new VRPTransformation(locations);
		
	}

	private void makeLocations(LocationsImpl locations) {
		locations.addAllLinks((Collection<Link>) network.getLinks().values());
	}

	public CarrierPlan buildPlan(CarrierCapabilities carrierCapabilities, Collection<CarrierContract> contracts) {
		logger.info("build plan");
		logger.info(contracts.size() + " number of contracts");
		if(contracts.isEmpty()){
			return getEmptyPlan(carrierCapabilities);
		}
		if(timeClusters != null){
			clusterContracts(contracts);
		}
		Collection<Tour> tours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		Collection<vrp.basics.Tour> vrpSolution = new ArrayList<vrp.basics.Tour>();
		RuinAndRecreateSolver ruinAndRecreateSolver = new RuinAndRecreateSolver(vrpSolution, vrpTrafo);
		ruinAndRecreateSolver.solve(contracts, carrierCapabilities.getCarrierVehicles().iterator().next());
		for(CarrierVehicle carrierVehicle : carrierCapabilities.getCarrierVehicles()){
			TourBuilder tourBuilder = new TourBuilder();
			Id vehicleStartLocation = carrierVehicle.getLocation();
			tourBuilder.scheduleStart(vehicleStartLocation);
			for(vrp.basics.Tour tour : vrpSolution){
				List<TourElement> enRouteActivities = new ArrayList<Tour.TourElement>();
				for(TourActivity act : tour.getActivities()){
					CarrierShipment shipment = getShipment(act.getCustomer());
					if(act instanceof vrp.basics.EnRouteDelivery){
						enRouteActivities.add(new Delivery(shipment));
					}
					if(act instanceof vrp.basics.EnRoutePickup){
						enRouteActivities.add(new Pickup(shipment));
					}
				}
				List<TourElement> tourActivities = new ArrayList<Tour.TourElement>();
				tourActivities.addAll(enRouteActivities);
				for(TourElement e : tourActivities){
					tourBuilder.schedule(e);
				}
			}
			tourBuilder.scheduleEnd(vehicleStartLocation);
			Tour tour = tourBuilder.build();
			tours.add(tour);
			ScheduledTour scheduledTour = new ScheduledTour(tour, carrierVehicle, 0.0);
			scheduledTours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		carrierPlan.setScore(ruinAndRecreateSolver.getVrpSolution().getTransportCosts());
		return carrierPlan;
	}
		

	private void clusterContracts(Collection<CarrierContract> contracts) {
		for(CarrierContract c : contracts){
			TimeCluster timeCluster = timeClusters.getCluster(c.getShipment().getPickupTimeWindow().getStart());
			timeCluster.contracts.add(c);
		}
	}

	private CarrierShipment getShipment(Customer customer) {
		return vrpTrafo.getShipment(makeId(customer.getId()));
	}

	private Id makeId(String id) {
		return new IdImpl(id);
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
