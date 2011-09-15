package playground.michalm.vrp.supply;

import java.util.ArrayList;
import java.util.Arrays;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;

import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule;
import pl.poznan.put.vrp.dynamic.data.schedule.ServeTask;
import pl.poznan.put.vrp.dynamic.data.schedule.Task;
import pl.poznan.put.vrp.dynamic.data.schedule.WaitTask;
import playground.michalm.vrp.data.MATSimVRPData;
import playground.michalm.vrp.data.network.MATSimVertex;
import playground.michalm.vrp.data.network.ShortestPath;
import playground.michalm.vrp.data.network.ShortestPath.SPEntry;


public class VRPSchedulePlan
    extends PlanImpl
{
    private PopulationFactory populFactory;
    private Network network;
    private NetworkFactoryImpl networkFactory;
    private ShortestPath[][] shortestPaths;

    private Schedule schedule;


    public VRPSchedulePlan(Person driver, Schedule schedule, MATSimVRPData data)
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

        Leg leg = populFactory.createLeg(TransportMode.car);

        leg.setDepartureTime(departTime);

        Link fromLink = fromVertex.getLink();
        Link toLink = toVertex.getLink();

        SPEntry entry = sp.getSPEntry(departTime);
        Id[] linkIds = entry.linkIds;

        NetworkRoute netRoute = (NetworkRoute)((PopulationFactoryImpl) populFactory).createRoute(TransportMode.car,
                fromLink.getId(), toLink.getId());

        if (linkIds.length > 0) {// means: fromLink != toLink

            // all except the last one (it's the toLink)
            ArrayList<Id> linkIdList = new ArrayList<Id>(linkIds.length - 1);

            for (int i = 0; i < linkIds.length - 1; i++) {
                linkIdList.add(linkIds[i]);
            }

            netRoute.setLinkIds(fromLink.getId(), Arrays.asList(entry.linkIds), toLink.getId());
            netRoute.setDistance(RouteUtils.calcDistance(netRoute, network));
        }
        else {
            netRoute.setDistance(0.0);
        }

        int travelTime = arrivalTime - departTime;// According to the route

        netRoute.setTravelTime(travelTime);
        netRoute.setTravelCost(entry.travelCost);

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
