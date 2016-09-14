package playground.sebhoerl.avtaxi.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;

public class AVTaxiScoringFunctionFactory implements ScoringFunctionFactory {
    private ScoringFunctionFactory standardFactory;
    private CharyparNagelScoringParametersForPerson params;
    
    public AVTaxiScoringFunctionFactory(Scenario scenario) {
        params = new SubpopulationCharyparNagelScoringParameters(scenario);
        standardFactory = new CharyparNagelScoringFunctionFactory(scenario);
    }
    
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		SumScoringFunction sf = (SumScoringFunction) standardFactory.createNewScoringFunction(person);
		
		double marginalUtilityOfWaiting = params.getScoringParameters(person).marginalUtilityOfWaiting_s;
		double marginalUtilityOfTraveling = params.getScoringParameters(person)
                .modeParams.get(TaxiModule.TAXI_MODE).marginalUtilityOfTraveling_s;
		
		sf.addScoringFunction(new AVTaxiWaitingTimeScoring(marginalUtilityOfWaiting, marginalUtilityOfTraveling));
		
		return sf;
	}
}
