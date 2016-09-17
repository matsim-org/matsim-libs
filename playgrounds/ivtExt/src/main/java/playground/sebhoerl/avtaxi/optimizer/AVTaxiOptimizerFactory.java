package playground.sebhoerl.avtaxi.optimizer;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.core.config.ConfigGroup;

import com.google.inject.Inject;

public class AVTaxiOptimizerFactory implements TaxiOptimizerFactory {
	final private TaxiSchedulerParams params;
	
	@Inject
	public AVTaxiOptimizerFactory(TaxiConfigGroup config) {
		this.params = new TaxiSchedulerParams(config);
	}
	
	@Override
	public TaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimContext, ConfigGroup optimizerConfigGroup) {
		Configuration optimizerConfig = new MapConfiguration(optimizerConfigGroup.getParams());
		return new AVHeuristicOptimizer(optimContext, params, new AVAggregateHeuristicOptimizerParams(optimizerConfig));
	}
}
