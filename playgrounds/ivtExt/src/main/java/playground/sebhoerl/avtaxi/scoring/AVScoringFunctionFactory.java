package playground.sebhoerl.avtaxi.scoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import playground.sebhoerl.avtaxi.framework.AVModule;

@Singleton
public class AVScoringFunctionFactory implements ScoringFunctionFactory {
    private ScoringFunctionFactory standardFactory;
    private CharyparNagelScoringParametersForPerson params;

	@Inject
    public AVScoringFunctionFactory(Scenario scenario) {
        params = new SubpopulationCharyparNagelScoringParameters(scenario);
        standardFactory = new CharyparNagelScoringFunctionFactory(scenario);
    }
    
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction sf = (SumScoringFunction) standardFactory.createNewScoringFunction(person);
		
		double marginalUtilityOfWaiting = params.getScoringParameters(person).marginalUtilityOfWaiting_s;
		double marginalUtilityOfTraveling = params.getScoringParameters(person)
                .modeParams.get(AVModule.AV_MODE).marginalUtilityOfTraveling_s;
		
		sf.addScoringFunction(new AVWaitingTimeScoring(marginalUtilityOfWaiting, marginalUtilityOfTraveling));
		
		return sf;
	}
}
