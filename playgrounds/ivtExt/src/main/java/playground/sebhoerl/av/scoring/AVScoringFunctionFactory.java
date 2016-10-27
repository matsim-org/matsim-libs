package playground.sebhoerl.av.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;

import playground.sebhoerl.av.framework.AVConfigGroup;
import playground.sebhoerl.av.framework.AVModule;

public class AVScoringFunctionFactory implements ScoringFunctionFactory {
    private ScoringFunctionFactory standardFactory;
    private CharyparNagelScoringParametersForPerson params;
    private AVConfigGroup config;
    
    public AVScoringFunctionFactory(Scenario scenario, AVConfigGroup config) {
        params = new SubpopulationCharyparNagelScoringParameters(scenario);
        standardFactory = new CharyparNagelScoringFunctionFactory(scenario);
        this.config = config;
    }
    
    @Override
    public ScoringFunction createNewScoringFunction(Person person) {
        SumScoringFunction sf = (SumScoringFunction) standardFactory.createNewScoringFunction(person);
        
        double marginalUtilityOfWaiting = config.getMarginalUtilityOfWaiting_s();
        double marginalUtilityOfTraveling = params.getScoringParameters(person)
                .modeParams.get(AVModule.AV_MODE).marginalUtilityOfTraveling_s;
        
        sf.addScoringFunction(new WaitingTimeScoring(
                marginalUtilityOfWaiting, marginalUtilityOfTraveling));
        
        return sf;
    }
}
