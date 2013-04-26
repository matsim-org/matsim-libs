package playground.jbischoff.taxi.optimizer;

import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiOptimizationPolicy;


public abstract class AbstractRankTaxiOptimizerFactory implements
		RankTaxiOptimizerFactory {
	
	    private final TaxiOptimizationPolicy optimizationPolicy;


	    public AbstractRankTaxiOptimizerFactory(TaxiOptimizationPolicy optimizationPolicy)
	    {
	        this.optimizationPolicy = optimizationPolicy;
	    }


	    @Override
	    public TaxiOptimizationPolicy getOptimizationPolicy()
	    {
	        return optimizationPolicy;
	    }
	}