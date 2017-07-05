/**
 * 
 */
package playground.joel.analysis;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataUtils;

/**
 * @author Claudio Ruch
 *
 */
public class MinimumFleetSizeCalculator {
    final Network network;
    final Population population;
    final VirtualNetwork virtualNetwork;
    final TravelData tData;
    final int numberTimeSteps;
    final int dt;

    public MinimumFleetSizeCalculator(Network networkIn, Population populationIn, VirtualNetwork virtualNetworkIn,int dtIn) {
        int dayduration = 30 * 60 * 60;
        dt = TravelDataUtils.greatestNonRestDt(dtIn, 30 * 60 * 60);
        numberTimeSteps = dayduration / dt;
        
        network = networkIn;
        population = populationIn;
        virtualNetwork = virtualNetworkIn;
        tData = new TravelData(virtualNetwork, network, population, dt);
    }

    public Tensor calculateMinFleet() {
        // ensure that dayduration / timeInterval is integer value
        

        Tensor minFleet = Tensors.empty();
        for (int k = 0; k < numberTimeSteps; ++k) {
            // extract all relevant requests
            ArrayList<RequestObj> relevantRequests = getRelevantRequests(population, k * dt, (k + 1) * dt);

            // arrival rate
            double lambdak = RequestAnalysis.calcArrivalRate(relevantRequests, dt);

            // calculate average trip distance
            double dODk = RequestAnalysis.calcavDistance(relevantRequests);

            // calculate earth movers distance
            //FIXME time and timesteps wrong
            double EMDk = RequestAnalysis.calcEMD(tData,virtualNetwork, dt, dt+1);

            // calculate average speed
            double vk = RequestAnalysis.calcAVSpeed(relevantRequests);

            // compute minimal fleet size
            double minFleetSizek = (lambdak * (dODk + EMDk)) / vk;
            minFleet.append(RealScalar.of(minFleetSizek));

        }
        return minFleet;
    }

    /**
     * @param population
     * @param timeStart
     * @param timeEnd
     * @return AV served requests in with submission time in interval [timeStart, timeEnd]
     */
    ArrayList<RequestObj> getRelevantRequests(Population population, double timeStart, double timeEnd) {
        ArrayList<RequestObj> returnRequests = new ArrayList<>();

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                PlanElement pE1 = null;
                PlanElement pE2 = null;
                PlanElement pE3 = null;

                for (PlanElement planElem : plan.getPlanElements()) {
                    pE1 = pE2;
                    pE2 = pE3;
                    pE3 = planElem;

                    if (pE2 instanceof Leg) {
                        Leg leg = (Leg) pE2;
                        double submissionTime = leg.getDepartureTime();
                        if (timeStart <= submissionTime && submissionTime < timeEnd) {


                            Activity a1 = (Activity) pE1;
                            Activity a3 = (Activity) pE3;

                            Id<Link> startLinkID = a1.getLinkId();
                            Id<Link> endLinkID = a3.getLinkId();

                            Link startLink = network.getLinks().get(startLinkID);
                            Link endLink = network.getLinks().get(endLinkID);

                            returnRequests.add(new RequestObj(submissionTime, startLink, endLink));

                        }

                    }
                }

            }

        }

        return returnRequests;

    }

}
