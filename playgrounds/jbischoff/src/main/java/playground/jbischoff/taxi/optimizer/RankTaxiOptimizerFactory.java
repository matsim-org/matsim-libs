package playground.jbischoff.taxi.optimizer;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.optimizer.VrpOptimizerFactory;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizationPolicy;

public interface RankTaxiOptimizerFactory extends VrpOptimizerFactory {

	
	   RankTaxiOptimizer create(VrpData data);
	   TaxiOptimizationPolicy getOptimizationPolicy();
}
