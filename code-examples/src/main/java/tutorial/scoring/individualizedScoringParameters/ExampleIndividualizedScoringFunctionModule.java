package tutorial.scoring.individualizedScoringParameters;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

/**
 * @author thibautd
 */
public class ExampleIndividualizedScoringFunctionModule extends AbstractModule {
    @Override
    public void install() {
        bindScoringFunctionFactory().to(CharyparNagelScoringFunctionFactory.class);
        bind(ScoringParametersForPerson.class).to(ExampleIndividualizedScoringParametersPerPerson.class);
    }
}
