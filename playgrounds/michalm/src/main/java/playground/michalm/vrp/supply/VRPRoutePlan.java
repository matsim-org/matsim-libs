package playground.michalm.vrp.supply;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.*;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.*;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.model.Route;
import pl.poznan.put.vrp.dynamic.data.network.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;


public class VRPRoutePlan
    extends PlanImpl
{
    private MATSimVRPData data;

    private PopulationFactory populFactory;
    private Network network;
    private NetworkFactoryImpl networkFactory;
    private ShortestPath[][] shortestPaths;
    private ArcTime[][] arcTimes;

    private Route vrpRoute;


    public VRPRoutePlan(Person driver, Route vrpRoute, MATSimVRPData data)
    {
        super(driver);
        this.vrpRoute = vrpRoute;
        this.data = data;

        populFactory = data.getScenario().getPopulation().getFactory();
        network = data.getScenario().getNetwork();
        networkFactory = (NetworkFactoryImpl)network.getFactory();
        shortestPaths = data.getShortestPaths();
        arcTimes = data.getVrpData().getVrpGraph().getTimes();

        init();
    }


    public Route getVrpRoute()
    {
        return vrpRoute;
    }


    private void init()
    {
        MATSimVertex depotVertex = (MATSimVertex)vrpRoute.vehicle.depot.vertex;

        if (vrpRoute.isUnplanned()) {// vehicle stays at the depot
            // Activity
            addActivity(depotVertex, -1, "RtU", null);

            return;
        }

        List<Request> reqs = vrpRoute.getRequests();

        // starts from the depot
        MATSimVertex prevVertex = (MATSimVertex)depotVertex;
        int departTime = vrpRoute.beginTime;

        // Activity
        addActivity(depotVertex, departTime, "RtP", null);

        for (int i = 0; i < reqs.size(); i++) {
            Request req = reqs.get(i);
            MATSimVertex currVertex = (MATSimVertex)req.fromVertex;

            // Leg
            addLeg(prevVertex, currVertex, departTime, req.arrivalTime);

            if (req.fromVertex != req.toVertex) { // i.e. taxi service
                System.err.println("DOESN'T WORK - ACT-LEG PAIRS!!!");
                // Leg
                currVertex = (MATSimVertex)req.toVertex;
                addLeg((MATSimVertex)req.fromVertex, currVertex, req.startTime, req.finishTime);
            }
            // else { //THIS DOES NOT WORK! LEG-ACT-LEG-ACT PAIRS MUST BE IN PLAN.........
            //
            // addActivity(currVertex, req.finishTime, "Request_" + req.id);
            // }
            //
            // // Activity
            // addActivity(currVertex, req.departureTime, "W");

            // Activity (Request + possible Waiting)
            addActivity(currVertex, req.departureTime, "" + req.id, req);

            prevVertex = currVertex;
            departTime = req.departureTime;
        }

        // returns to the depot

        // Leg
        int travelTime = arcTimes[prevVertex.getId()][depotVertex.getId()]
                .getArcTimeOnDeparture(departTime);
        addLeg(prevVertex, depotVertex, departTime, departTime + travelTime);

        // Activity
        addActivity(depotVertex, -1, "RtC", null);
    }


    private void addLeg(MATSimVertex fromVertex, MATSimVertex toVertex, int departTime,
            int arrivalTime)
    {
        ShortestPath sp = shortestPaths[fromVertex.getId()][toVertex.getId()];

        if (sp == ShortestPath.NO_SHORTEST_PATH) {
            throw new RuntimeException("No route found from vertex " + fromVertex + " to vertex "
                    + toVertex);
        }

        Leg leg = populFactory.createLeg(TransportMode.car);

        leg.setDepartureTime(departTime);

        Link fromLink = fromVertex.getLink();
        Link toLink = toVertex.getLink();

        Path path = sp.getPath(departTime);

        NetworkRoute netRoute = (NetworkRoute)networkFactory.createRoute(TransportMode.car,
                fromLink.getId(), toLink.getId());

        if (path != ShortestPath.ZERO_PATH) {// means: fromLink != toLink
            netRoute.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links),
                    toLink.getId());

            netRoute.setDistance(RouteUtils.calcDistance(netRoute, network));
        }
        else {
            netRoute.setDistance(0.0);
        }

        int travelTime = arrivalTime - departTime;// According to the route

        netRoute.setTravelTime(travelTime);
        netRoute.setTravelCost(path.travelCost);

        leg.setRoute(netRoute);
        leg.setDepartureTime(departTime);
        leg.setTravelTime(travelTime);
        ((LegImpl)leg).setArrivalTime(arrivalTime);

        addLeg(leg);
    }


    // it is posssible only to update the future elements of the plan
    public void updatePlan()
    {

    }


    private void addActivity(MATSimVertex vertex, int endTime, String type, Request vrpRequest)
    {
        // Activity act = populFactory.createActivityFromLinkId("service",
        // vertex.getLink().getId());
        Activity act = new ActivityImpl(type, vertex.getCoord(), vertex.getLink().getId());

        if (endTime != -1) {
            act.setEndTime(endTime);
        }

        addActivity(act);
    }
}
