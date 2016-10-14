package playground.sebhoerl.avtaxi.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;
import playground.sebhoerl.avtaxi.data.AVOperator;

public class AVRoute extends AbstractRoute {
    final static String AV_ROUTE = "av";

    private Id<AVOperator> operatorId;

    public AVRoute(Id<Link> startLinkId, Id<Link> endLinkId, Id<AVOperator> operatorId) {
        super(startLinkId, endLinkId);
        this.operatorId = operatorId;
    }

    @Override
    public String getRouteDescription() {
        return operatorId.toString();
    }

    @Override
    public void setRouteDescription(String routeDescription) {
        operatorId = Id.create(routeDescription, AVOperator.class);
    }

    @Override
    public String getRouteType() {
        return AV_ROUTE;
    }

    public Id<AVOperator> getOperatorId() {
        return operatorId;
    }
    public void setOperatorId(Id<AVOperator> operatorId) { this.operatorId = operatorId; }
}
