package playground.artemc.heterogeneity.scoring;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;

/**
 * Created by artemc on 24/06/16.
 */
public class HeterogeneousScoringFunctionModule extends AbstractModule {

	@Override
	public void install() {
		bindScoringFunctionFactory().to(CharyparNagelScoringFunctionFactory.class);
		bind(CharyparNagelScoringParametersForPerson.class).to(HeterogeneousSubpopulationScoringParameters.class);
	}
}
