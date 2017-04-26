package playground.ivt.matsim2030.scoring;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;

/**
 * @author thibautd
 */
public class MATSim2010ScoringModule extends AbstractModule {
	@Override
	public void install() {
		bind(ScoringParametersForPerson.class).to( MATSim2010ScoringParametersPerPerson.class );
		bindScoringFunctionFactory().to( ExternalParametersMatsim2010ScoringFunctionFactory.class );
	}
}
