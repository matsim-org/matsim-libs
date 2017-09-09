// code by clruch
package playground.clruch.prep;

import java.io.File;
import java.util.Comparator;
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

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Sort;
import ch.ethz.idsc.tensor.io.Export;
import ch.ethz.idsc.tensor.io.Import;

public class PopulationRequestSchedule {
    static final String VIRTUALNETWORK_DIRECTORYNAME = "virtualNetwork";

    @Deprecated
    static File getRequestScheduleFileNext() { // not needed for MPC
        return new File(VIRTUALNETWORK_DIRECTORYNAME, "mpcRequestScheduleNext.csv");
    }

    public static File getRequestScheduleFileGlobal() {
        return new File(VIRTUALNETWORK_DIRECTORYNAME, "PopulationRequestSchedule.csv");
    }

    private static Comparator<Tensor> FIRSTENTRYCOMPARATOR = new Comparator<Tensor>() {
        @Override
        public int compare(Tensor o1, Tensor o2) {
            return Scalars.compare(o1.Get(0), o2.Get(0));
        }
    };

    final VirtualNetwork virtualNetwork;
    final Tensor requestScheduleSorted;

    public PopulationRequestSchedule(Network network, Population population, VirtualNetwork virtualNetwork) {
        this.virtualNetwork = virtualNetwork;
        Tensor requestScheduleUnsorted = Tensors.empty();
        Map<Id<Link>, ? extends Link> linkMap = network.getLinks();

        int personCount = 0;
        for (Person person : population.getPersons().values()) {
            // System.out.println(person);
            int planCount = 0;
            // for (Plan plan : person.getPlans()) {
            // System.out.println(" " + plan);
            Plan plan = person.getSelectedPlan();
            StdRequest std = null;
            for (PlanElement pE1 : plan.getPlanElements()) {
                // System.out.println(" " + pE1);
                if (pE1 instanceof Activity) {
                    Activity activity = (Activity) pE1;
                    Link link = linkMap.get(activity.getLinkId());
                    if (std != null) {
                        std.post = link;
                        requestScheduleUnsorted.append(requestRow(std));
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
            // }
            if (planCount != 1) {
                // System.out.println("person with plans: " + planCount);
            }
            ++personCount;
        }
        requestScheduleSorted = Sort.of(requestScheduleUnsorted, FIRSTENTRYCOMPARATOR);
        System.out.println("PopulationRequestSchedule ----");
        System.out.println("#Person: " + personCount);
        System.out.println("#Trips : " + requestScheduleUnsorted.length());
        if (0 < requestScheduleSorted.length()) {
            System.out.println(" \\- first = " + requestScheduleSorted.get(0));
            System.out.println(" \\- last  = " + requestScheduleSorted.get(requestScheduleSorted.length() - 1));
        }
    }

    private Tensor requestRow(StdRequest std) {
        long time = Math.round(std.departureTime);
        int vnAnte = virtualNetwork.getVirtualNode(std.ante).getIndex() + 1; // +1 for indexing in matlab
        int vnPost = virtualNetwork.getVirtualNode(std.post).getIndex() + 1;
        return Tensors.vector(time, vnAnte, vnPost);
    }

    public Tensor get() {
        return requestScheduleSorted.unmodifiable();
    }

    public void exportCsv() throws Exception {
        Export.of(getRequestScheduleFileGlobal(), requestScheduleSorted);
    }

    public static Tensor importDefault() throws Exception {
        return Import.of(getRequestScheduleFileGlobal());
    }
}
