package playground.michalm.vrp.supply;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.*;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.*;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.*;

import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.vrp.data.*;
import playground.michalm.vrp.data.network.*;


public class VRPRoutePlan
    extends PlanImpl
{
    private PopulationFactory populFactory;
    private Network network;
    private NetworkFactoryImpl networkFactory;
    private ShortestPath[][] shortestPaths;

    private Schedule schedule;


    public VRPRoutePlan(Person driver, Schedule schedule, MATSimVRPData data)
    {
        super(driver);
        this.schedule = schedule;

        populFactory = data.getScenario().getPopulation().getFactory();
        network = data.getScenario().getNetwork();
        networkFactory = (NetworkFactoryImpl)network.getFactory();
        shortestPaths = data.getShortestPaths();

        init();
    }


    public Schedule getSchedule()
    {
        return schedule;
    }


    private void init()
    {
        MATSimVertex depotVertex = (MATSimVertex)schedule.getVehicle().depot.vertex;

        if (schedule.getStatus().isUnplanned()) {// vehicle stays at the depot
            addActivity(depotVertex, -1, "RtU");
            return;
        }

        // Depot - before schedule.getBeginTime()
        addActivity(depotVertex, schedule.getBeginTime(), "RtP");

        for (Task t : schedule.getTasks()) {
            switch (t.getType()) {
                case DRIVE:
                    DriveTask dt = (DriveTask)t;
                    addLeg((MATSimVertex)dt.getFromVertex(), (MATSimVertex)dt.getToVertex(),
                            dt.getBeginTime(), dt.getEndTime());
                    break;

                case SERVE:
                    ServeTask st = (ServeTask)t;
                    addActivity((MATSimVertex)st.getAtVertex(), st.getEndTime(),
                            "" + st.getRequest().id);
                    break;

                case WAIT:
                    WaitTask wt = (WaitTask)t;
                    addActivity((MATSimVertex)wt.getAtVertex(), wt.getEndTime(), "W");
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        // Depot - after schedule.getEndTime()
        addActivity(depotVertex, -1, "RtC");
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


    private void addActivity(MATSimVertex vertex, int endTime, String type)
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
