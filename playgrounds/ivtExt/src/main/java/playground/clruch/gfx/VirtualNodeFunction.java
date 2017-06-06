// code by jph
package playground.clruch.gfx;

import ch.ethz.idsc.tensor.Tensor;
import playground.clruch.net.SimulationObject;

public interface VirtualNodeFunction {
    Tensor evaluate(SimulationObject ref);
}
