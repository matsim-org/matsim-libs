/**
 * 
 */
package playground.clruch.analysis.minimumfleetsize;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.analysis.AnalysisUtils;
import playground.clruch.analysis.AnalyzeAll;
import playground.clruch.analysis.DiagramCreator;
import playground.clruch.analysis.RequestAnalysis;
import playground.clruch.analysis.RequestObj;
import playground.clruch.html.DataCollector;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.clruch.utils.GlobalAssert;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.joel.helpers.EasyDijkstra;

/** @author Claudio Ruch */
public class MinimumFleetSizeCalculator implements Serializable {
    final int numberTimeSteps;
    final int dt;
    final public static Tensor minFleet = Tensors.empty();
    final public static Tensor EMDks = Tensors.empty();
    public double minimumFleet;

    public MinimumFleetSizeCalculator(Network network, Population population, VirtualNetwork virtualNetwork, TravelData travelData) {
        numberTimeSteps = travelData.getNumbertimeSteps();
        GlobalAssert.that(108000 % numberTimeSteps == 0);
        dt = travelData.getdt();
        calculateMinFleet(network, population, virtualNetwork, travelData);
    }

    private Tensor calculateMinFleet(Network network, Population population, VirtualNetwork virtualNetwork, TravelData travelData) {

        LeastCostPathCalculator dijkstra = EasyDijkstra.prepDijkstra(network);

        double num = 0.0;
        double den = 0.0;
        Tensor totalArrivals = RealScalar.ZERO;
        for (int k = 0; k < numberTimeSteps; ++k) {
            // extract all relevant requests
            ArrayList<RequestObj> relevantRequests = getRelevantRequests(population, network, k * dt, (k + 1) * dt);

            // arrival rate
            double lambdak = RequestAnalysis.calcArrivalRate(relevantRequests, dt);
            Tensor totalArrivalsDT = RealScalar.of(lambdak).multiply(RealScalar.of(dt));
            totalArrivals = totalArrivals.add(totalArrivalsDT);

            // calculate average trip distance
            double dODk = RequestAnalysis.calcavDistance(dijkstra, relevantRequests);

            // calculate earth movers distance
            // FIXME time and timesteps wrong, was: (..., dt, dt + 1)
            double EMDk = RequestAnalysis.calcEMD(travelData, virtualNetwork, dijkstra, dt, k * dt);
            double EMDkav = EMDk / relevantRequests.size(); // EMD per request
            EMDks.append(RealScalar.of(EMDk));

            // calculate average speed
            double vk = RequestAnalysis.calcAVSpeed(dijkstra, relevantRequests);

            // compute minimal fleet size
            double minFleetSizek = vk == 0.0 ? 0.0 : (lambdak * (dODk + EMDkav)) / vk;
            minFleet.append(RealScalar.of(minFleetSizek));
            num += (minFleetSizek * vk);
            den += vk;

        }
        System.out.println("total arrivals for calculating minimum fleet size: " + totalArrivals);
        minimumFleet = num / den;
        return minFleet;
    }

    /** @param population
     * @param timeStart
     * @param timeEnd
     * @return AV served requests in with submission time in interval [timeStart, timeEnd] */
    ArrayList<RequestObj> getRelevantRequests(Population population, Network network, double timeStart, double timeEnd) {
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
                        if (leg.getMode().equals("av")) {
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
        }
        return returnRequests;
    }

    public void plot(String[] args) throws Exception {
        DiagramCreator.createDiagram(AnalyzeAll.RELATIVE_DIRECTORY, "EMD", "Earth Movers Distance", //
                DataCollector.loadScenarioData(args).EMDks.multiply(RealScalar.of(0.001)), "km");
        DiagramCreator.createDiagram(AnalyzeAll.RELATIVE_DIRECTORY, "minFleet", "Minimum Fleet Size", //
                DataCollector.loadScenarioData(args).minFleet, "vehicles");
    }
    
    public double getMinFleet(){
        return minimumFleet;
    }

    /* package */ void checkConsistency() {
        // TODO implement this

    }

    public static void main(String[] args) throws Exception {

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        File configFile = new File(args[0]);
        Config config = ConfigUtils.loadConfig(configFile.toString(), new playground.sebhoerl.avtaxi.framework.AVConfigGroup(), dvrpConfigGroup,
                new BlackListedTimeAllocationMutatorConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();

        VirtualNetwork virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        TravelData travelData = TravelDataGet.readDefault(virtualNetwork);

        MinimumFleetSizeCalculator minimumFleetSizeCalculator = new MinimumFleetSizeCalculator(network, population, virtualNetwork, travelData);

        // System.setOut(originalStream);
        Tensor minFleet = minimumFleetSizeCalculator.calculateMinFleet(network, population, virtualNetwork, travelData);
        ;

        System.out.println("Population size: " + population.getPersons().size());
        System.out.println("Earth movers distances: " + EMDks);
        System.out.println("Minimallly required fleet sizes " + minFleet);
        double minVeh = AnalysisUtils.maximum(minFleet).number().doubleValue();
        System.out.println(minVeh + " -> " + Math.ceil(minVeh));
        System.out.println("min Fleet size: " + minimumFleetSizeCalculator.getMinFleet());
    }

}
