package playground.ciarif.carpooling;

import org.matsim.core.config.Config;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;

import playground.meisterk.kti.config.KtiConfigGroup;

public class LegScoringFunction extends playground.meisterk.kti.scoring.LegScoringFunction{

	public LegScoringFunction(PlanImpl plan,
			CharyparNagelScoringParameters params, Config config,
			KtiConfigGroup ktiConfigGroup) {
		super(plan, params, config, ktiConfigGroup);
		// TODO Auto-generated constructor stub
	}

}
