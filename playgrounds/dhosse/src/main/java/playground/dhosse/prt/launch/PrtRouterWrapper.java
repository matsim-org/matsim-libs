package playground.dhosse.prt.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.old.LegRouter;
import org.matsim.pt.PtConstants;

public class PrtRouterWrapper implements RoutingModule {

	private static final StageActivityTypes CHECKER = 
			new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
	private final String mode;
	private final PopulationFactory populationFactory;
	private final PrtNetworkLegRouter wrapped;
	
	public PrtRouterWrapper(final String mode, final PopulationFactory populationFactory, final PrtNetworkLegRouter prtNetworkLegRouter){
		this.mode = mode;
		this.populationFactory = populationFactory;
		this.wrapped = prtNetworkLegRouter;
	}
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		
		List<Leg> trip = this.wrapped.calcRoute(fromFacility.getCoord(), toFacility.getCoord(),
				departureTime, person);
		
		return fillWithActivities(trip, fromFacility, toFacility, departureTime, person);
		
	}

	private List<? extends PlanElement> fillWithActivities(List<Leg> baseTrip,
			Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		
		List<PlanElement> trip = new ArrayList<PlanElement>();
		
		//walk leg
		trip.add(baseTrip.get(0));
		
		if(baseTrip.size() < 2) return trip;
		else{
		
		//interaction
		Activity act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, baseTrip.get(0).getRoute().getEndLinkId());
		act.setMaximumDuration(0);
		trip.add(act);
		
		//prt leg
		trip.add(baseTrip.get(1));
		
		//interaction
		act = new ActivityImpl(PtConstants.TRANSIT_ACTIVITY_TYPE, baseTrip.get(1).getRoute().getEndLinkId());
		act.setMaximumDuration(0);
		trip.add(act);
		
		//walk leg
		trip.add(baseTrip.get(2));
		
		return trip;
		}
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

}
