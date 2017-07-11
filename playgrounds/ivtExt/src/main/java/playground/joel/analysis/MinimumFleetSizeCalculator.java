/**
 * 
 */
package playground.joel.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

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
<<<<<<< HEAD

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
=======
>>>>>>> master
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
<<<<<<< HEAD
import org.matsim.core.network.io.MatsimNetworkReader;
=======
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
>>>>>>> master
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Mean;
import playground.clruch.netdata.KMEANSVirtualNetworkCreator;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.prep.TheApocalypse;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataUtils;
import playground.clruch.trb18.scenario.TRBScenarioConfig;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.joel.helpers.EasyDijkstra;
import playground.joel.html.DataCollector;

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
    final public static Tensor EMDks = Tensors.empty();
    public static double minimumFleet;

    public static void main(String[] args) throws Exception {
        // for test purpose only
        int samples = 30;

        /*
         * PrintStream originalStream = System.out; System.setOut(new PrintStream(new OutputStream(){
         * 
         * @Override public void write(int b) {} }));
         */

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        File configFile = new File(args[0]);
<<<<<<< HEAD
        Config config = ConfigUtils.loadConfig(configFile.toString(), new playground.sebhoerl.avtaxi.framework.AVConfigGroup(), dvrpConfigGroup, new BlackListedTimeAllocationMutatorConfigGroup());
        config.facilities().setInputFile(null);
=======
        Config config = ConfigUtils.loadConfig(configFile.toString(), new playground.sebhoerl.avtaxi.framework.AVConfigGroup(), dvrpConfigGroup,
                new BlackListedTimeAllocationMutatorConfigGroup());
>>>>>>> master
        Scenario scenario = ScenarioUtils.loadScenario(config);
        //Network network = scenario.getNetwork();

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(new TRBScenarioConfig().filteredNetworkOutputPath);

        Population population = scenario.getPopulation();
        TheApocalypse.decimatesThe(population).toNoMoreThan(1000).people();

        MinimumFleetSizeCalculator minimumFleetSizeCalculator = new MinimumFleetSizeCalculator(network, //
                population, 40, 108000 / samples);

        // System.setOut(originalStream);
        Tensor minFleet = minimumFleetSizeCalculator.calculateMinFleet();

        System.out.println("Population size: " + population.getPersons().size());
        System.out.println("Earth movers distances: " + EMDks);
        System.out.println("Minimallly required fleet sizes " + minFleet);
        double minVeh = AnalysisUtils.maximum(minFleet).number().doubleValue();
        System.out.println(minVeh + " -> " + Math.ceil(minVeh));
        System.out.println("min Fleet size: " + minimumFleet);
    }

    public MinimumFleetSizeCalculator(Network networkIn, Population populationIn, int numVirtualNodes, int dtIn) {
        population = populationIn;

        // create a network containing only car nodes
        System.out.println("number of links in original network: " + networkIn.getLinks().size());
        final TransportModeNetworkFilter filter = new TransportModeNetworkFilter(networkIn);
        network = NetworkUtils.createNetwork();
        filter.filter(network, Collections.singleton("car"));
        System.out.println("number of links in car network: " + network.getLinks().size());

        // create virtualNetwork based on input network
        KMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new KMEANSVirtualNetworkCreator();
        virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(population, network, numVirtualNodes, true);

        int dayduration = 108000;
        // ensure that dayduration / timeInterval is integer value
        dt = TravelDataUtils.greatestNonRestDt(dtIn, dayduration);
        numberTimeSteps = dayduration / dt;

        tData = new TravelData(virtualNetwork, network, population, dt);
    }

    public Tensor calculateMinFleet() {

        LeastCostPathCalculator dijkstra = EasyDijkstra.prepDijkstra(network);

        Tensor minFleet = Tensors.empty();
        double num = 0.0;
        double den = 0.0;
        for (int k = 0; k < numberTimeSteps; ++k) {
            // extract all relevant requests
            ArrayList<RequestObj> relevantRequests = getRelevantRequests(population, k * dt, (k + 1) * dt);

            // arrival rate
            double lambdak = RequestAnalysis.calcArrivalRate(relevantRequests, dt);

            // calculate average trip distance
            double dODk = RequestAnalysis.calcavDistance(dijkstra, relevantRequests);

            // calculate earth movers distance
            // FIXME time and timesteps wrong, was: (..., dt, dt + 1)
            double EMDk = RequestAnalysis.calcEMD(tData, virtualNetwork, dijkstra, dt, k * dt);
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
        minimumFleet = num/den;
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

                        if (leg.getMode().equals("av")) { // FIX: Only AV legs!!!
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

}
