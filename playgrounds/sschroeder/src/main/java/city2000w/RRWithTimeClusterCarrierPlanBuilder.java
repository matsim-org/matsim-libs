package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierContract;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierShipment;
import playground.mzilske.freight.CarrierVehicle;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;
import playground.mzilske.freight.Tour;
import playground.mzilske.freight.TourBuilder;
import freight.TourScheduler;
import freight.utils.GreedyShipmentAggregator;
import freight.utils.ShipmentClustererImpl;
import freight.utils.TimePeriod;
import freight.utils.TimePeriods;
import freight.utils.TimeSchema;
import freight.vrp.VRPSolver;
import freight.vrp.VRPSolverFactory;

public class RRWithTimeClusterCarrierPlanBuilder {
	
	static class Week extends TimePeriods{
		public Week(){
			addTimePeriod(TimeSchema.MONDAY);
			addTimePeriod(TimeSchema.TUESDAY);
			addTimePeriod(TimeSchema.WEDNESDAY);
			addTimePeriod(TimeSchema.THURSDAY);
			addTimePeriod(TimeSchema.FRIDAY);
			addTimePeriod(TimeSchema.SATURDAY);
		}
	}
	
	private static Logger logger = Logger.getLogger(RRWithTimeClusterCarrierPlanBuilder.class);
	
	private Network network;
	
	private GreedyShipmentAggregator greedyShipmentAggregator;
	
	private VRPSolverFactory solverFactory;
	
	private TourScheduler tourScheduler;
	
	public RRWithTimeClusterCarrierPlanBuilder(Network network, VRPSolverFactory solverFactory){
		this.network = network;
		this.solverFactory = solverFactory;
	}

	public void setShipmentFilter(ShipmentFilter shipmentFilter) {
		this.shipmentFilter = shipmentFilter;
	}

	public void setWeek(){
		timePeriods = new Week();
	}

	public void setTimePeriods(TimePeriods clusters){
		timePeriods = clusters;
	}

	public void setTourScheduler(TourScheduler tourScheduler) {
		this.tourScheduler = tourScheduler;
	}

	public void setGreedyShipmentAggregator(
			GreedyShipmentAggregator greedyShipmentAggregator) {
		this.greedyShipmentAggregator = greedyShipmentAggregator;
	}

	public CarrierPlan buildPlan(CarrierCapabilities carrierCapabilities, Collection<CarrierContract> contracts) {
		logger.info("build plan");
		logger.info(contracts.size() + " number of contracts");
		if(contracts.isEmpty()){
			return getEmptyPlan(carrierCapabilities);
		}
		ShipmentClustererImpl shipmentClusterer = new ShipmentClustererImpl(timePeriods);
		Map<TimePeriod, Collection<Shipment>> clusteredShipments = shipmentClusterer.clusterShipments(getShipments(contracts));
		
		Collection<ScheduledTour> scheduledTours = new ArrayList<ScheduledTour>();
		Double planScore = null;
		for(TimePeriod timePeriod : clusteredShipments.keySet()){
			Collection<CarrierShipment> shipments = filterAndGetShipments(clusteredShipments.get(timePeriod));
			if(shipments.isEmpty()){
				continue;
			}
			Map<CarrierShipment,Collection<CarrierShipment>> aggregatedShipments = greedyShipmentAggregator.aggregateShipments(shipments);
			VRPSolver vrpSolver = solverFactory.createSolver(aggregatedShipments.keySet(), carrierCapabilities.getCarrierVehicles(), network);
			Collection<Tour> tours = vrpSolver.solve();
			Collection<ScheduledTour> myScheduledTours = tourScheduler.getScheduledTours(tours, aggregatedShipments);
			scheduledTours.addAll(myScheduledTours);
		}
		CarrierPlan carrierPlan = new CarrierPlan(scheduledTours);
		carrierPlan.setScore(planScore);
		return carrierPlan;
	}

	private TimePeriods timePeriods = new Week();
	
	private ShipmentFilter shipmentFilter;
	
	private Collection<Shipment> getShipments(Collection<CarrierContract> contracts) {
		Collection<Shipment> shipments = new ArrayList<Shipment>();
		for(CarrierContract c : contracts){
			shipments.add(c.getShipment());
		}
		return shipments;
	}

	private Collection<CarrierShipment> filterAndGetShipments(Collection<Shipment> shipments) {
		Collection<CarrierShipment> shipments_ = new ArrayList<CarrierShipment>();
		for(Shipment shipment : shipments){
			if(shipmentFilter.judge((CarrierShipment)shipment)){
				shipments_.add((CarrierShipment)shipment);
			}
		}
		return shipments_;
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
