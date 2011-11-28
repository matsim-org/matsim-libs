package playground.michalm.vrp.sim;

import pl.poznan.put.vrp.dynamic.data.*;
import pl.poznan.put.vrp.dynamic.optimizer.*;


public interface VRPOptimizerFactory
{
    VRPOptimizer create(VRPData data);
}
