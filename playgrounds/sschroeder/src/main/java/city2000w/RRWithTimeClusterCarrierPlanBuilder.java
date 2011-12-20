package city2000w;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierContract;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Shipment;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.TourBuilder;
import org.matsim.contrib.freight.vrp.VRPSolver;
import org.matsim.contrib.freight.vrp.VRPSolverFactory;
import org.matsim.contrib.freight.vrp.basics.Coordinate;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.CrowFlyCosts;
import org.matsim.contrib.freight.vrp.basics.Locations;
import org.matsim.core.basic.v01.IdImpl;

import freight.TourScheduler;
import freight.utils.GreedyShipmentAggregator;
import freight.utils.ShipmentClustererImpl;
import freight.utils.TimePeriod;
import freight.utils.TimePeriods;
import freight.utils.TimeSchema;

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
		CrowFlyCosts crowFlyDistance = new CrowFlyCosts(new Locations(){

			@Override
			public Coordinate getCoord(String id) {
				return makeCoordinate(network.getLinks().get(makeId(id)).getCoord());
			}
			
			private Coordinate makeCoordinate(Coord coord) {
				return new Coordinate(coord.getX(),coord.getY());
			}

			public Id makeId(String id){
				return new IdImpl(id);
			}
			
		});
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
			crowFlyDistance.speed = 18;
			crowFlyDistance.detourFactor = 1.2;
			VRPSolver vrpSolver = solverFactory.createSolver(aggregatedShipments.keySet(), carrierCapabilities.getCarrierVehicles(), network, crowFlyDistance);
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
