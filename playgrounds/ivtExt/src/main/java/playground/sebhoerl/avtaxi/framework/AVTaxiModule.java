package playground.sebhoerl.avtaxi.framework;

import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.core.controler.AbstractModule;

import playground.sebhoerl.avtaxi.optimizer.AVTaxiOptimizerFactory;

public class AVTaxiModule extends AbstractModule {
	@Override
	public void install() {
		bind(TaxiOptimizerFactory.class).to(AVTaxiOptimizerFactory.class);
	}
}
