package playground.sebhoerl.avtaxi;

import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.core.config.ConfigGroup;

public class AVTaxiOptimizerFactory implements TaxiOptimizerFactory {

	@Override
	public TaxiOptimizer createTaxiOptimizer(TaxiOptimizerContext optimContext, ConfigGroup optimizerConfigGroup) {
		return new AVTaxiOptimizer();
	}

}
