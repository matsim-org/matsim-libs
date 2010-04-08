package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.population.routes.RouteWRefs;

import playground.meisterk.kti.router.KtiPtRoute;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;

public class FtCarSharingRouteFactory implements RouteFactory {

	private static final long serialVersionUID = 1L;

	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo;

	public FtCarSharingRouteFactory(PlansCalcRouteKtiInfo plansCalcRouteKtiInfo) {
		super();
		this.plansCalcRouteKtiInfo = plansCalcRouteKtiInfo;
	}

	@Override
	public RouteWRefs createRoute(Id startLinkId, Id endLinkId) {
		return new KtiPtRoute(startLinkId, endLinkId, this.plansCalcRouteKtiInfo);
	}
}
