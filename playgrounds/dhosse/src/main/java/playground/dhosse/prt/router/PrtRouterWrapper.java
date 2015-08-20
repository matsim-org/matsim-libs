package playground.dhosse.prt.router;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.dhosse.prt.data.PrtData;
import playground.dhosse.prt.passenger.PrtRequestCreator;
import playground.michalm.taxi.data.ETaxiData;
import playground.michalm.taxi.data.TaxiRank;

public class PrtRouterWrapper implements RoutingModule {

	private RoutingModule walkRouter;
	private NetworkImpl network;
	private PrtData data;
	
	public PrtRouterWrapper(final String mode, Network network, final PopulationFactory populationFactory, 
			MatsimVrpContextImpl context, PrtData data, final RoutingModule routingModule){
		this.walkRouter = routingModule;
		this.network = (NetworkImpl) network;
		this.data = data;
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

		TaxiRank accessStop = this.data.getNearestRank(fromFacility.getCoord());
		TaxiRank egressStop = this.data.getNearestRank(toFacility.getCoord());

		TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
		TransitStopFacility accessFacility = factory.createTransitStopFacility(Id.create(accessStop.getId().toString(), TransitStopFacility.class), accessStop.getCoord(), false);
		accessFacility.setLinkId(accessStop.getLink().getId());
		TransitStopFacility egressFacility = factory.createTransitStopFacility(Id.create(egressStop.getId().toString(), TransitStopFacility.class), egressStop.getCoord(), false);
		egressFacility.setLinkId(egressStop.getLink().getId());
		
		//walk leg
		Leg leg = (Leg) this.walkRouter.calcRoute(fromFacility, accessFacility, time, person).get(0);
		trip.add(leg);
		time += leg.getTravelTime();
        
        //pt interaction
        Activity act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, accessStop.getLink().getId());
		act.setMaximumDuration(60);
		trip.add(act);
		time += act.getMaximumDuration();
        
        //prtLeg
		leg = new LegImpl(PrtRequestCreator.MODE);
		GenericRoute route = new GenericRouteImpl(accessFacility.getLinkId(), egressFacility.getLinkId());
		leg.setRoute(route);
		leg.setDepartureTime(time);
        trip.add(leg);
        time += leg.getTravelTime();
		
		//interaction
		act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, egressStop.getLink().getId());
		act.setMaximumDuration(0);
		trip.add(act);
		
		//walk leg
		leg = (Leg) this.walkRouter.calcRoute(fromFacility, accessFacility, time, person).get(0);
        trip.add(leg);
		
		return trip;
		
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

}
