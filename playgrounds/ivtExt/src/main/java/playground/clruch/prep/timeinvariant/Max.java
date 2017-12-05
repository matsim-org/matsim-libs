/**
 * 
 */
package playground.clruch.prep.timeinvariant;

import ch.ethz.idsc.tensor.RationalScalar;
import ch.ethz.idsc.tensor.Tensor;

/**
 * @author Claudio Ruch
 *
 */
public class Max {
    ;
    
    public static Tensor of(Tensor... tensors) {
        Tensor[] tensorsM = new Tensor[tensors.length];
        for(int i = 0; i<tensors.length;++i){
            tensorsM[i] = tensors[i].multiply(RationalScalar.of(-1, 1));
        }
        Tensor maxM = Min.of(tensorsM);
        return maxM.multiply(RationalScalar.of(-1, 1));
        
    }

}
