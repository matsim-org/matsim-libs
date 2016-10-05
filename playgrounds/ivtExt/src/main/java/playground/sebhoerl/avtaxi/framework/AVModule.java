package playground.sebhoerl.avtaxi.framework;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import playground.sebhoerl.avtaxi.refactor.AVTaxiOptimizerFactory;
import playground.sebhoerl.avtaxi.scoring.AVScoringFunctionFactory;

public class AVModule extends AbstractModule {
    final static String AV_MODE = "av";

	@Override
	public void install() {
		bind(TaxiOptimizerFactory.class).to(AVTaxiOptimizerFactory.class);
	}
	
    @Provides @Singleton
    public ScoringFunctionFactory provideAVTaxiScoringFunctionFactory(Scenario scenario) {
        return new AVScoringFunctionFactory(scenario);
    }
}
