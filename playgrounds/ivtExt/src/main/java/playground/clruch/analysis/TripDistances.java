/**
 * 
 */
package playground.clruch.analysis;

import java.io.File;
import java.util.ArrayList;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.util.LeastCostPathCalculator;

import ch.ethz.idsc.queuey.math.AnalysisUtils;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import playground.clruch.traveldata.TravelData;

/** @author Claudio Ruch */
public class TripDistances {
    private Tensor tripDistances = Tensors.empty();
    private Tensor tripDistanceBinCounter = Tensors.empty();
    private Scalar tripDistanceBinSize = RealScalar.of(0.5); // minimally, in km
    private final int dt;
    private final int numberTimeSteps;
    // will be stepwise increased if too small

    public TripDistances(LeastCostPathCalculator dijkstra, TravelData travelData, //
            Population population, Network network, File relativeDirectory) throws Exception {
        numberTimeSteps = travelData.getNumbertimeSteps();
        dt = travelData.getdt();
        fill(dijkstra, population, network);
        analyze(relativeDirectory);
    }

    private void fill(LeastCostPathCalculator dijkstra, Population population, Network network) {

        for (int k = 0; k < numberTimeSteps; ++k) {
            ArrayList<RequestObj> relevantRequests = RequestAnalysis.getRelevantRequests(population, network, k * dt, (k + 1) * dt);

            for (RequestObj rObj : relevantRequests) {
                // double vlDist = actualDistance(rObj.fromLink.getCoord(), rObj.toLink.getCoord()); // euclidean approach
                double dist = RequestAnalysis.actualDistance(dijkstra, rObj.fromLink.getFromNode(), rObj.toLink.getToNode()); // dijkstra approach
                tripDistances.append(RealScalar.of(dist));
            }
        }

    }

    private void analyze(File relativeDirectory) throws Exception {
        tripDistances = tripDistances.multiply(RealScalar.of(0.001));

        tripDistanceBinSize = AnalysisUtils.adaptBinSize(tripDistances, tripDistanceBinSize, RealScalar.of(0.5));
        tripDistanceBinCounter = AnalysisUtils.binCount(tripDistances, tripDistanceBinSize);

        DiagramCreator.binCountGraph(relativeDirectory, "tripDistances", //
                "Trips per Distance", tripDistanceBinCounter, //
                tripDistanceBinSize.number().doubleValue(), 100.0 / tripDistances.length(), //
                "% of requests", "Distances according to Dijkstra", " km", //
                1000, 750);
    }

}
