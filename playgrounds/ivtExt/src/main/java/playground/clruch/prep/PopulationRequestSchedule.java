package playground.clruch.prep;

import java.io.File;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.Import;
import playground.clruch.netdata.VirtualNetwork;

public class PopulationRequestSchedule {
    static final String VIRTUALNETWORK_DIRECTORYNAME = "virtualNetwork";

    @Deprecated
    static File getRequestScheduleFileNext() { // not needed for MPC
        return new File(VIRTUALNETWORK_DIRECTORYNAME, "mpcRequestScheduleNext.csv");
    }

    public static File getRequestScheduleFileGlobal() {
        return new File(VIRTUALNETWORK_DIRECTORYNAME, "mpcRequestScheduleGlobal.csv");
    }

    final VirtualNetwork virtualNetwork;
    final Tensor requestSchedule = Tensors.empty();

    public PopulationRequestSchedule(Network network, Population population, VirtualNetwork virtualNetwork) {
        this.virtualNetwork = virtualNetwork;

        Map<Id<Link>, ? extends Link> linkMap = network.getLinks();

        int personCount = 0;
        for (Person person : population.getPersons().values()) {
            // System.out.println(person);
            int planCount = 0;
            for (Plan plan : person.getPlans()) {
                // System.out.println(" " + plan);
                StdRequest std = null;
                for (PlanElement pE1 : plan.getPlanElements()) {
                    // System.out.println(" " + pE1);
                    if (pE1 instanceof Activity) {
                        Activity activity = (Activity) pE1;
                        Link link = linkMap.get(activity.getLinkId());
                        if (std != null) {
                            std.to = link;
                            append(std);
                        }
                        std = new StdRequest(link);
                    }
                    if (pE1 instanceof Leg) {
                        Leg leg = (Leg) pE1;
                        leg.getMode().equals("av");
                        std.departureTime = leg.getDepartureTime();
                        // System.out.println(" " + pE1);
                    }
                }
                ++planCount;
            }
            if (planCount != 1) {
                System.out.println("person with plans: " + planCount);
            }
            ++personCount;
        }
        System.out.println("PopulationRequestSchedule ----");
        System.out.println("#Person: " + personCount);
        System.out.println("#Trips : " + requestSchedule.length());
    }

    private void append(StdRequest std) {
        long time = Math.round(std.departureTime);
        int vnFrom = virtualNetwork.getVirtualNode(std.from).index + 1;
        int vnTo = virtualNetwork.getVirtualNode(std.to).index + 1;
        Tensor row = Tensors.vector(time, vnFrom, vnTo);
        requestSchedule.append(row);
    }

    public void exportDefault() throws Exception {
        Export.of(getRequestScheduleFileGlobal(), requestSchedule);
    }

    public static Tensor importDefault() throws Exception {
        return Import.of(getRequestScheduleFileGlobal());
    }
}
