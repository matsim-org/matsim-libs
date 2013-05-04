package playground.jbischoff.taxi.optimizer;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.immediaterequest.TaxiOptimizationPolicy;

public interface RankTaxiOptimizerFactory {

	
	   RankTaxiOptimizer create(VrpData data);
	   TaxiOptimizationPolicy getOptimizationPolicy();
}
