package playground.dhosse.prt.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.router.VrpPathCalculatorImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.michalm.taxi.data.TaxiData;
import playground.michalm.taxi.data.TaxiRank;

public class PrtRouterWrapper implements RoutingModule {

	private static final StageActivityTypes CHECKER = 
			new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
	private RoutingModule walkRouter;
	private VrpPathCalculatorImpl calculator;
	private NetworkImpl network;
	private PrtData data;
	
	public PrtRouterWrapper(final String mode, Network network, final PopulationFactory populationFactory, 
			MatsimVrpContextImpl context, final VrpPathCalculatorImpl vrpPathCalculatorImpl, final RoutingModule routingModule){
		this.walkRouter = routingModule;
		this.calculator = vrpPathCalculatorImpl;
		this.network = (NetworkImpl) network;
		this.data = new PrtData(network, (TaxiData) context.getVrpData());
		double[] bounds = NetworkUtils.getBoundingBox(this.network.getNodes().values());
		this.data.initRankQuadTree(bounds);
	}
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {

		return fillWithActivities(null, fromFacility, toFacility, departureTime, person);
		
	}

	private List<? extends PlanElement> fillWithActivities(List<Leg> baseTrip,
			Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		
		double time = departureTime;
		
		List<PlanElement> trip = new ArrayList<PlanElement>();

		TaxiRank accessStop = PrtData.getNearestRank(fromFacility.getCoord());
		TaxiRank egressStop = PrtData.getNearestRank(toFacility.getCoord());

		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
		TransitStopFacility accessFacility = factory.createTransitStopFacility(Id.create(accessStop.getId().toString(), TransitStopFacility.class), accessStop.getCoord(), false);
		accessFacility.setLinkId(accessStop.getLink().getId());
		TransitStopFacility egressFacility = factory.createTransitStopFacility(Id.create(egressStop.getId().toString(), TransitStopFacility.class), egressStop.getCoord(), false);
		egressFacility.setLinkId(egressStop.getLink().getId());
		
		//walk leg
		Leg leg = new LegImpl(TransportMode.transit_walk);
		Route route = new GenericRouteImpl(fromFacility.getLinkId(), accessStop.getLink().getId());
		List<? extends PlanElement> walkRoute = this.walkRouter.calcRoute(fromFacility, accessFacility, departureTime, person);
		route.setDistance(((Leg) walkRoute.get(0)).getRoute().getDistance());
        route.setTravelTime(((Leg) walkRoute.get(0)).getRoute().getTravelTime());
        leg.setRoute(route);
        trip.add(leg);
        time += leg.getTravelTime();
        
        //pt interaction
        Activity act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, accessStop.getLink().getId());
		act.setMaximumDuration(0);
		trip.add(act);
        
        //prtLeg
//		VrpPathWithTravelData path = this.calculator.calcPath(this.network.getLinks().get(accessFacility.getLinkId()), 
//				this.network.getLinks().get(egressFacility.getLinkId()), time);
		leg = new LegImpl(PrtRequestCreator.MODE);
		route = new GenericRouteImpl(accessFacility.getLinkId(), egressFacility.getLinkId());
		route.setStartLinkId(accessFacility.getLinkId());
		route.setEndLinkId(egressFacility.getLinkId());
//		route.setTravelTime(path.getTravelTime());
//		route.setDistance(path.getTravelCost());
		leg.setRoute(route);
//		leg.setTravelTime(path.getTravelTime());
        trip.add(leg);
		
		//interaction
		act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, egressStop.getLink().getId());
		act.setMaximumDuration(0);
		trip.add(act);
		
		//walk leg
		leg = new LegImpl(TransportMode.transit_walk);
		route = new GenericRouteImpl(egressStop.getLink().getId(), toFacility.getLinkId());
		walkRoute = this.walkRouter.calcRoute(egressFacility, toFacility, departureTime, person);
		route.setDistance(((Leg) walkRoute.get(0)).getRoute().getDistance());
        route.setTravelTime(((Leg) walkRoute.get(0)).getRoute().getTravelTime());
        leg.setRoute(route);
        trip.add(leg);
		
		return trip;
		
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

}
