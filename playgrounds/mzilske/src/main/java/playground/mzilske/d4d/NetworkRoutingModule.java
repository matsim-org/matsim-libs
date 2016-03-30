package playground.mzilske.d4d;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;

public class NetworkRoutingModule implements RoutingModule {
	

	ModeRouteFactory mrf = new ModeRouteFactory();
	

	private RoutingModule networkLegRouter;

	public NetworkRoutingModule(PopulationFactory pf, Network network, TravelTime ttc) {
		super();
//		mrf.setRouteFactory("unknown", new GenericRouteFactory());
		networkLegRouter = DefaultRoutingModules.createPureNetworkRouter("unknown", pf, network, new Dijkstra(network, new OnlyTimeDependentTravelDisutility(ttc), ttc) );
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
		List<? extends PlanElement> onLeg = networkLegRouter.calcRoute(fromFacility, toFacility, departureTime, person);
		((Leg) onLeg.get(0)).setMode("car");
		return onLeg;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {

		return EmptyStageActivityTypes.INSTANCE;
	}

}
