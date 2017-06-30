package playground.joel.analysis;

import ch.ethz.idsc.tensor.DoubleScalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;


import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by Joel on 29.06.2017.
 */
public class AnalysisUtils {

    static double maximum(Tensor submissions){
        double max = submissions.Get(0).number().doubleValue();
        for (int i = 1; i < submissions.length(); i++) {
            double current = submissions.Get(i).number().doubleValue();
            if (max < current)
                max = current;
        }
        return max;
    }

    static Tensor binCounter(Tensor tensor, double binSize) {
        Tensor binCounter = Tensors.empty();
        NavigableMap<Double, Integer> counter = new TreeMap<>();
        for (int i = 0; i*binSize < maximum(tensor); i++) counter.put(i*binSize, 0);
        for (int j = 0; j < tensor.length(); j++) {
            int current = counter.floorEntry(tensor.Get(j).number().doubleValue()).getValue();
            counter.put(counter.floorKey(tensor.Get(j).number().doubleValue()), current + 1);
        }
        counter.values().forEach(k -> binCounter.append(DoubleScalar.of(k)));
        return binCounter;
    }

}
