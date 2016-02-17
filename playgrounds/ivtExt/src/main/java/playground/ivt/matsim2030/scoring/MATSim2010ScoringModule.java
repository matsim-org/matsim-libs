package playground.ivt.matsim2030.scoring;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;

/**
 * @author thibautd
 */
public class MATSim2010ScoringModule extends AbstractModule {
	@Override
	public void install() {
		bind(CharyparNagelScoringParametersForPerson.class).to( MATSim2010ScoringParametersPerPerson.class );
		bindScoringFunctionFactory().to( ExternalParametersMatsim2010ScoringFunctionFactory.class );
	}
}
