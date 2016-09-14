package playground.sebhoerl.avtaxi.framework;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import playground.sebhoerl.avtaxi.optimizer.AVTaxiOptimizerFactory;
import playground.sebhoerl.avtaxi.scoring.AVTaxiScoringFunctionFactory;

public class AVTaxiModule extends AbstractModule {
	@Override
	public void install() {
		bind(TaxiOptimizerFactory.class).to(AVTaxiOptimizerFactory.class);
	}
	
    @Provides @Singleton
    public ScoringFunctionFactory provideAVTaxiScoringFunctionFactory(Scenario scenario) {
        return new AVTaxiScoringFunctionFactory(scenario);
    }
}
