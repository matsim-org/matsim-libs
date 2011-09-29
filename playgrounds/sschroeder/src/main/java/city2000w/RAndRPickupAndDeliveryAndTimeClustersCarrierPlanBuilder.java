package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierContract;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierPlanBuilder;
import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.TimeWindow;
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

public class RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder implements CarrierPlanBuilder {
	
private static Logger logger = Logger.getLogger(RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder.class);
	
	public static class Schedule {
		private Collection<CarrierContract> contracts;
		
		private double start;

		public Schedule(Collection<CarrierContract> contracts, double start) {
			super();
			this.contracts = contracts;
			this.start = start;
		}

		public Collection<CarrierContract> getContracts() {
			return contracts;
		}

		public double getStart() {
			return start;
		}
	}
	
	static class TourCluster {
		private Collection<CarrierContract> contracts;
		
		private Id clusterId;

		public TourCluster(Id clusterId, Collection<CarrierContract> contracts) {
			super();
			this.contracts = contracts;
			this.clusterId = clusterId;
		}

		public Id getClusterId() {
			return clusterId;
		}

		public Collection<CarrierContract> getContracts() {
			return contracts;
		}
	}
	
	public static double WORKING_PERIOD = 12*3600; 

	private Network network;
	
	private VRPTransformation vrpTrafo;
	
	public RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder(Network network){
		this.network = network;
	}

	private void iniTrafo() {
		LocationsImpl locations = new LocationsImpl();
		makeLocations(locations);
		vrpTrafo = new VRPTransformation(locations);
	}

	private void makeLocations(LocationsImpl locations) {
		locations.addAllLinks((Collection<Link>) network.getLinks().values());
	}

	/* (non-Javadoc)
	 * @see city2000w.CarrierPlanBuilder#buildPlan(playground.mzilske.freight.CarrierCapabilities, java.util.Collection)
	 */
	@Override
	public CarrierPlan buildPlan(CarrierCapabilities carrierCapabilities, Collection<CarrierContract> carrierContracts) {
		if(carrierContracts.isEmpty()){
			return getEmptyPlan(carrierCapabilities);
		}
		iniTrafo();
		double planScore = 0.0;
		Collection<Tour> planTours = new ArrayList<Tour>();
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		List<Schedule> schedules = getSchedules(carrierContracts);
		CarrierVehicle carrierVehicle = carrierCapabilities.getCarrierVehicles().iterator().next();
		for(Schedule schedule : schedules){
			Collection<CarrierContract> contracts = schedule.getContracts();
			if(contracts.isEmpty()){
				continue;
			}
			List<TourCluster> clusters = getTourCluster(contracts);
			
			TourBuilder tourBuilder = new TourBuilder();
			Id vehicleStartLocation = carrierVehicle.getLocation();
			tourBuilder.scheduleStart(vehicleStartLocation);
			for(TourCluster tourCluster : clusters){
				Collection<CarrierContract> clusteredContracts = tourCluster.getContracts();
				if(clusteredContracts.isEmpty()){
					continue;
				}
				Collection<vrp.basics.Tour> vrpSolution = new ArrayList<vrp.basics.Tour>();
				RuinAndRecreateSolver ruinAndRecreateSolver = new RuinAndRecreateSolver(vrpSolution, vrpTrafo);
				ruinAndRecreateSolver.solve(clusteredContracts, carrierVehicle);
				planScore += ruinAndRecreateSolver.getVrpSolution().getTransportCosts();
				
				for(vrp.basics.Tour tour : vrpSolution){
					for(TourActivity act : tour.getActivities()){
						CarrierShipment shipment = getShipment(act.getCustomer());
						if(act instanceof vrp.basics.EnRouteDelivery){
							assertShipmentNotNull(shipment);
							tourBuilder.schedule(new Delivery(shipment));
						}
						if(act instanceof vrp.basics.EnRoutePickup){
							assertShipmentNotNull(shipment);
							tourBuilder.schedule(new Pickup(shipment));
						}
					}
				}
			}
			
			tourBuilder.scheduleEnd(vehicleStartLocation);
			Tour tour = tourBuilder.build();
			assertSequenceOfTourElementsCorrect(tour.getTourElements(), carrierContracts, clusters, schedules);
			planTours.add(tour);
			ScheduledTour scheduledTour = new ScheduledTour(tour, carrierVehicle, schedule.getStart());
			scheduledTours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		carrierPlan.setScore(planScore);
		return carrierPlan;
	}


	private void assertShipmentNotNull(Shipment shipment) {
		if(shipment == null){
			throw new IllegalStateException("shipment is null. this should not be.");
		}
		return;
	}

	private List<TourCluster> getTourCluster(Collection<CarrierContract> contracts) {
		Collection<CarrierContract> vorlauf = new ArrayList<CarrierContract>();
		Collection<CarrierContract> nachlauf = new ArrayList<CarrierContract>();
		for(CarrierContract c : contracts){
			if(isVorlauf(c.getShipment())){
				vorlauf.add(c);
			}
			else{
				nachlauf.add(c);
			}
		}
		List<TourCluster> clusters = new ArrayList<RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder.TourCluster>();
		clusters.add(new TourCluster(new IdImpl("vorlauf"), vorlauf));
		clusters.add(new TourCluster(new IdImpl("nachlauf"), nachlauf));
		return clusters;
	}

	private boolean isVorlauf(Shipment shipment) {
		//start="0.0" end="0.0"
		//start="86400.0" end="86400.0"
		if(shipment.getFrom().toString().equals("industry")){
			return true;
		}
		return false;
	}


	private void assertSequenceOfTourElementsCorrect(List<TourElement> tourActivities, Collection<CarrierContract> carrierContracts, List<TourCluster> clusters, List<Schedule> schedules) {
		TimeWindow tw = null;
		for(TourElement e : tourActivities){
			if(tw == null){
				tw = e.getTimeWindow();
			}
			else{
				if(e.getShipment() != null){
					if(e.getTimeWindow().getStart() >= tw.getStart()){
						tw = e.getTimeWindow();
					}
					else{
						throw new IllegalStateException("this should not be. tour acts not in a correct sequence");
					}
				}
			}
		}
		
	}

	private List<Schedule> getSchedules(Collection<CarrierContract> carrierContracts) {
		List<Schedule> schedules = new ArrayList<RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder.Schedule>();
		List<CarrierContract> forenoon = new ArrayList<CarrierContract>();
		List<CarrierContract> afternoon = new ArrayList<CarrierContract>();
		for(CarrierContract c : carrierContracts){
			if(c.getShipment().getPickupTimeWindow().getStart() < WORKING_PERIOD){
				forenoon.add(c);
			}
			else{
				afternoon.add(c);
			}
		}
		schedules.add(new Schedule(forenoon,0.0));
		schedules.add(new Schedule(afternoon,WORKING_PERIOD));
		return schedules;
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
