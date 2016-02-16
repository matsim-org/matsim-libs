package playground.mzilske.d4d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.Facility;

public class BushwhackingRoutingModule implements RoutingModule {

	private NetworkImpl network;
	
	ModeRouteFactory mrf = new ModeRouteFactory();
	
	private RoutingModule teleportationLegRouter;

	private RoutingModule networkLegRouter;

	public BushwhackingRoutingModule(PopulationFactory pf, NetworkImpl network) {
		this.network = network;
		ModeRoutingParams params = new ModeRoutingParams() ;
		params.setMode("unknown");
		params.setBeelineDistanceFactor(1.7);
		params.setTeleportedModeSpeed(2.0);
		teleportationLegRouter = DefaultRoutingModules.createTeleportationRouter("unknown", pf, params ) ; 
		TravelTime ttc = new FreeSpeedTravelTime();
		networkLegRouter = DefaultRoutingModules.createPureNetworkRouter("unknown", pf, 
				network, new Dijkstra(network, new OnlyTimeDependentTravelDisutility(ttc), ttc) );
	}

	@Override
	public List<PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		
		final Link fromLink = NetworkUtils.getNearestLink(network, fromFacility.getCoord());
		final Link toLink = NetworkUtils.getNearestLink(network, toFacility.getCoord());
		
		Facility linkEnterFacility = new Facility() {

			@Override
			public Coord getCoord() {
				return fromLink.getFromNode().getCoord();
			}

			@Override
			public Id getId() {
				return null;
			}

			@Override
			public Map<String, Object> getCustomAttributes() {
				return null;
			}

			@Override
			public Id getLinkId() {
				return fromLink.getId();
			}
			
		};
		
		Facility linkLeaveFacility = new Facility() {
			
			@Override
			public Coord getCoord() {
				return toLink.getToNode().getCoord();
			}

			@Override
			public Id getId() {
				return null;
			}

			@Override
			public Map<String, Object> getCustomAttributes() {
				return null;
			}

			@Override
			public Id getLinkId() {
				return toLink.getId();
			}
			
		};

		
		List<? extends PlanElement> inLeg = teleportationLegRouter.calcRoute(fromFacility, linkEnterFacility, departureTime, person);
		
		double inLegTravelTime = ((Leg) inLeg.get(0)).getTravelTime();
		List<? extends PlanElement> onLeg = networkLegRouter.calcRoute(linkEnterFacility, linkLeaveFacility, departureTime + inLegTravelTime, person);
		((Leg) onLeg.get(0)).setMode("car");
		
		double onLegTravelTime = ((Leg) onLeg.get(0)).getTravelTime();
		List<? extends PlanElement> outLeg = teleportationLegRouter.calcRoute(linkLeaveFacility, toFacility, departureTime + inLegTravelTime + onLegTravelTime, person);
		
		double outLegTravelTime = ((Leg) outLeg.get(0)).getTravelTime();
		double travelTime = inLegTravelTime + onLegTravelTime + outLegTravelTime;
		
		List<? extends PlanElement> justWalk = teleportationLegRouter.calcRoute(fromFacility, toFacility, departureTime, person);
		double justWalkTravelTime = ((Leg) justWalk.get(0)).getTravelTime();
		
		
		if (justWalkTravelTime <= travelTime) {
			List<PlanElement> route = new ArrayList<PlanElement>();
			route.addAll(justWalk);
			return route;
		} else {
			List<PlanElement> route = new ArrayList<PlanElement>();
			route.addAll(inLeg);
			route.addAll(onLeg);
			route.addAll(outLeg);
			return route;
		}
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}

}
