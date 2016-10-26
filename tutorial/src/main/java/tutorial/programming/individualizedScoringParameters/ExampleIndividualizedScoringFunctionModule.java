package tutorial.programming.individualizedScoringParameters;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;

/**
 * @author thibautd
 */
public class ExampleIndividualizedScoringFunctionModule extends AbstractModule {
    @Override
    public void install() {
        bindScoringFunctionFactory().to(CharyparNagelScoringFunctionFactory.class);
        bind(CharyparNagelScoringParametersForPerson.class).to(ExampleIndividualizedScoringParametersPerPerson.class);
    }
}
