package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.TourBuilder;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreate;
import vrp.algorithms.ruinAndRecreate.RuinAndRecreateFactory;
import vrp.algorithms.ruinAndRecreate.constraints.TimeAndCapacityPickupsDeliveriesSequenceConstraint;
import vrp.api.Constraints;
import vrp.api.Customer;
import vrp.api.VRP;
import vrp.basics.CrowFlyDistance;
import vrp.basics.TourActivity;
import vrp.basics.VrpUtils;
import freight.CarrierUtils;
import freight.vrp.Locations;
import freight.vrp.VRPTransformation;
import freight.vrp.VrpBuilder;

public class RRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder {
	
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
		
		public Iterator<TimeCluster> iterator();
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

		@Override
		public Iterator<TimeCluster> iterator() {
			return clusters.values().iterator();
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
	
	private static Logger logger = Logger.getLogger(RRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder.class);
	
	private Network network;
	
	private VRPTransformation vrpTrafo;
	
	private CarrierImpl carrier;
	
	public void setCarrier(CarrierImpl carrier) {
		this.carrier = carrier;
	}

	private Clusters timeClusters = new WeekClusters();
	
	public void setWeek(){
		timeClusters = new WeekClusters();
	}
	
	public void setTimeClusters(Clusters clusters){
		timeClusters = clusters;
	}

	public RRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder(Network network){
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
			if(counter > 1){
				break;
			}
			counter++;
		
			TimeCluster cluster = clusterIter.next();
			Collection<vrp.basics.Tour> vrpSolution = new ArrayList<vrp.basics.Tour>();
			VRPTransformation vrpTransformation = new VRPTransformation(new Locations(){

				@Override
				public Coord getCoord(Id id) {
					return network.getLinks().get(id).getCoord();
				}
				
			});
			List<CarrierVehicle> carrierVehicles = new ArrayList<CarrierVehicle>(carrierCapabilities.getCarrierVehicles());
			int usedVehicleCounter = 0;
			
			
			CarrierVehicle vehicle = carrierCapabilities.getCarrierVehicles().iterator().next();
			RuinAndRecreate ruinAndRecreate = prepareAlgo(vrpSolution,vrpTransformation,cluster.contracts,vehicle);
			ruinAndRecreate.run();
			if(planScore == null){
				planScore = ruinAndRecreate.getVrpSolution().getTransportCosts();
			}
			else{
				planScore += ruinAndRecreate.getVrpSolution().getTransportCosts();
			}
			for(vrp.basics.Tour tour : vrpSolution){
				TourBuilder tourBuilder = new TourBuilder();
				boolean tourStarted = false;
				double start = cluster.timeBin.start; 
				for(TourActivity act : tour.getActivities()){
					Shipment shipment = getShipment(act.getCustomer());
					if(act instanceof vrp.basics.OtherDepotActivity){
						if(tourStarted){
							tourBuilder.scheduleEnd(makeId(act.getCustomer().getLocation().getId()));
						}
						else{
							tourStarted = true;
							tourBuilder.scheduleStart(makeId(act.getCustomer().getLocation().getId()));
							start = act.getCustomer().getTheoreticalTimeWindow().getStart();
						}
					}
					if(act instanceof vrp.basics.EnRouteDelivery){
						tourBuilder.scheduleDelivery(shipment);
					}
					if(act instanceof vrp.basics.EnRoutePickup){
						tourBuilder.schedulePickup(shipment);
					}
				}
				Tour vehicleTour = tourBuilder.build();
				CarrierVehicle availableVehicle = null;
				if(usedVehicleCounter < carrierVehicles.size()){
					availableVehicle = carrierVehicles.get(usedVehicleCounter);
					usedVehicleCounter++;
				}
				else{
					availableVehicle = CarrierUtils.createAndAddVehicle(carrier, getVehicleId(carrierVehicles), vehicle.getLocation().toString(), vehicle.getCapacity());
				}
				scheduledTours.add(new ScheduledTour(vehicleTour, availableVehicle, start));
			}
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		carrierPlan.setScore(planScore);
		return carrierPlan;
	}
		

	private RuinAndRecreate prepareAlgo(Collection<vrp.basics.Tour> vrpSolution, VRPTransformation vrpTransformation, Collection<Contract> contracts, CarrierVehicle carrierVehicle) {
		Id depotId = carrierVehicle.getLocation();
		VrpBuilder vrpBuilder = new VrpBuilder(depotId);
		CrowFlyDistance costs = new CrowFlyDistance();
		costs.speed = 25;
		vrpBuilder.setCosts(costs);
		Constraints constraints = new TimeAndCapacityPickupsDeliveriesSequenceConstraint(carrierVehicle.getCapacity(),8*3600,costs);
		vrpBuilder.setConstraints(constraints);
		for(Contract c : contracts){
			Shipment s = c.getShipment();
			vrpTransformation.addShipment(s);
		}
		vrpBuilder.setVRPTransformation(vrpTransformation);
		VRP vrp = vrpBuilder.buildVRP();
		RuinAndRecreateFactory rrFactory = new RuinAndRecreateFactory();
		rrFactory.setWarmUp(4);
		rrFactory.setIterations(20);
		Collection<vrp.basics.Tour> initialSolution = VrpUtils.createTrivialSolution(vrp);
		RuinAndRecreate ruinAndRecreateAlgo = rrFactory.createStandardAlgo(vrp, initialSolution, carrierVehicle.getCapacity());
		return ruinAndRecreateAlgo;
	}

	private String getVehicleId(List<CarrierVehicle> vehicles) {
		return "veh_" + carrier.getId().toString() + "_" + (vehicles.size()+1);
	}

	private void clusterContracts(Collection<Contract> contracts) {
		for(Contract c : contracts){
			TimeCluster timeCluster = timeClusters.getCluster(c.getShipment().getPickupTimeWindow().getStart());
			timeCluster.contracts.add(c);
		}
	}

	private Shipment getShipment(Customer customer) {
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
