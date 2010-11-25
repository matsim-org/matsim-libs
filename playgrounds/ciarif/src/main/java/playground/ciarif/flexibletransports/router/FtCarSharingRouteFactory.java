package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactory;

public class FtCarSharingRouteFactory
  implements RouteFactory
{
  private final PlansCalcRouteFtInfo plansCalcRouteFtInfo;

  public FtCarSharingRouteFactory(PlansCalcRouteFtInfo plansCalcRouteFtInfo)
  {
    this.plansCalcRouteFtInfo = plansCalcRouteFtInfo;
  }

  public Route createRoute(Id startLinkId, Id endLinkId)
  {
    return new FtCarSharingRoute(startLinkId, endLinkId, this.plansCalcRouteFtInfo);
  }
}
