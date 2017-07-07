package playground.joel.analysis;

import ch.ethz.idsc.tensor.*;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Tally;
import ch.ethz.idsc.tensor.sca.Floor;


import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by Joel on 29.06.2017.
 */
public class AnalysisUtils {

    static Scalar maximum(Tensor submissions){
        return submissions.flatten(0).reduce(Max::of).get().Get();
    }


    static Tensor binCount(Tensor tensor, Scalar binSize) {
        Tensor floor = Floor.of(tensor.multiply(binSize.invert()));
        Map<Tensor, Long> map = Tally.of(floor);
        Scalar max = maximum(floor);
        return Tensors.vector(i->map.containsKey(RealScalar.of(i)) ? //
                RealScalar.of(map.get(RealScalar.of(i))) : RealScalar.ZERO, max.number().intValue()+1);

    }

}
