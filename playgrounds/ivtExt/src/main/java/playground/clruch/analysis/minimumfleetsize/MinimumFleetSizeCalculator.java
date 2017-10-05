/**
 * 
 */
package playground.clruch.analysis.minimumfleetsize;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.math.AnalysisUtils;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.analysis.DiagramCreator;
import playground.clruch.analysis.RequestAnalysis;
import playground.clruch.analysis.RequestObj;
import playground.clruch.netdata.VirtualNetworkGet;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataGet;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;
import playground.joel.helpers.EasyDijkstra;

/** @author Claudio Ruch */
public class MinimumFleetSizeCalculator implements Serializable {
    final int numberTimeSteps;
    final int dt;
    private Tensor minFleet;
    private Tensor EMDks;
    public double minimumFleet;

    public MinimumFleetSizeCalculator(Network network, Population population, VirtualNetwork<Link> virtualNetwork, TravelData travelData) {
        // data preparation
        numberTimeSteps = travelData.getNumbertimeSteps();
        GlobalAssert.that(108000 % numberTimeSteps == 0);
        dt = travelData.getdt();
        EMDks = Tensors.empty();
        minFleet = Tensors.empty();
        // calculation
        calculateMinFleet(network, population, virtualNetwork, travelData);
    }

    private Tensor calculateMinFleet(Network network, Population population, VirtualNetwork<Link> virtualNetwork, TravelData travelData) {

        LeastCostPathCalculator dijkstra = EasyDijkstra.prepDijkstra(network);

        double num = 0.0;
        double den = 0.0;
        Tensor totalArrivals = RealScalar.ZERO;
        for (int k = 0; k < numberTimeSteps; ++k) {
            // extract all relevant requests
            ArrayList<RequestObj> relevantRequests = RequestAnalysis.getRelevantRequests(population, network, k * dt, (k + 1) * dt);

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

    public void plot(File relativeDirectory) throws Exception {
        DiagramCreator.createDiagram(relativeDirectory, "EMD", "Earth Movers Distance", //
                EMDks.multiply(RealScalar.of(0.001)), "km");
        DiagramCreator.createDiagram(relativeDirectory, "minFleet", "Minimum Fleet Size", //
                minFleet, "vehicles");
        // DataCollector.loadScenarioData(args).minFleet, "vehicles");
    }

    public Tensor getMinFleet() {
        return minFleet.copy();
    }

    public double getMinFleetNumber() {
        return minimumFleet;
    }

    public Tensor getEMDk() {
        return EMDks.copy();
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

        VirtualNetwork<Link> virtualNetwork = VirtualNetworkGet.readDefault(scenario.getNetwork());
        TravelData travelData = TravelDataGet.readDefault(virtualNetwork);

        MinimumFleetSizeCalculator minimumFleetSizeCalculator = new MinimumFleetSizeCalculator(network, population, virtualNetwork, travelData);

        // System.setOut(originalStream);
        Tensor minFleet = minimumFleetSizeCalculator.calculateMinFleet(network, population, virtualNetwork, travelData);
        ;

        System.out.println("Population size: " + population.getPersons().size());
        System.out.println("Earth movers distances: " + minimumFleetSizeCalculator.getEMDk());
        System.out.println("Minimallly required fleet sizes " + minFleet);
        double minVeh = AnalysisUtils.maximum(minFleet).number().doubleValue();
        System.out.println(minVeh + " -> " + Math.ceil(minVeh));
        System.out.println("min Fleet size: " + minimumFleetSizeCalculator.getMinFleetNumber());
    }

}
