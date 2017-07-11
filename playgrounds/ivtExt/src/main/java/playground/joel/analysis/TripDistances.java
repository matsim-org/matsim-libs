package playground.joel.analysis;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;

/**
 * Created by Joel on 10.07.2017.
 */
public class TripDistances {
    public static Tensor tripDistances = Tensors.empty();
    public static Tensor tripDistanceBinCounter = Tensors.empty();

    public static Scalar tripDistanceBinSize = RealScalar.of(0.5); // minimally, in km
    // will be stepwise increased if too small

    public static void analyze() throws  Exception {
        tripDistances = tripDistances.multiply(RealScalar.of(0.001));

        tripDistanceBinSize = AnalysisUtils.adaptBinSize(tripDistances, tripDistanceBinSize, RealScalar.of(0.5));
        tripDistanceBinCounter = AnalysisUtils.binCount(tripDistances, tripDistanceBinSize);

        DiagramCreator.binCountGraph(AnalyzeAll.RELATIVE_DIRECTORY, "tripDistances", //
                "Trips per Distance", TripDistances.tripDistanceBinCounter, //
                TripDistances.tripDistanceBinSize.number().doubleValue(), 100.0/tripDistances.length(), //
                "% of requests", "Distances according to Dijkstra", " km", //
                1000, 750);
    }

}
