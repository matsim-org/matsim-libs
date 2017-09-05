package playground.clruch.analysis;

import java.util.Map;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.red.Max;
import ch.ethz.idsc.tensor.red.Tally;
import ch.ethz.idsc.tensor.sca.Floor;

/**
 * Created by Joel on 29.06.2017.
 */
public class AnalysisUtils {

    public static Scalar maximum(Tensor submissions){
        return submissions.flatten(-1).reduce(Max::of).get().Get();
    }


    static Tensor binCount(Tensor tensor, Scalar binSize) {
        Tensor floor = Floor.of(tensor.divide(binSize));
        Map<Tensor, Long> map = Tally.of(floor);
        Scalar max = maximum(floor);
        return Tensors.vector(i->map.containsKey(RealScalar.of(i)) ? //
                RealScalar.of(map.get(RealScalar.of(i))) : RealScalar.ZERO, max.number().intValue()+1);

    }

    static Scalar adaptBinSize(Tensor tensor, Scalar binSize, Scalar step) {
        if(AnalysisUtils.maximum(tensor).divide(binSize).number().doubleValue() > 40.0) {
            binSize = binSize.add(step);
            binSize = adaptBinSize(tensor, binSize, step);
        }
        return binSize;
    }

}
