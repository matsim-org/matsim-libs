package playground.sebhoerl.avtaxi.scoring;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
<<<<<<< HEAD
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
=======
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;

import com.google.inject.Inject;
import com.google.inject.Singleton;

>>>>>>> master
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVModule;

@Singleton
public class AVScoringFunctionFactory implements ScoringFunctionFactory {
<<<<<<< HEAD
	final private AVConfig config;
	final private ScoringFunctionFactory standardFactory;
	final private ScoringParametersForPerson params;
=======
    final private AVConfig config;
    final private ScoringFunctionFactory standardFactory;
    final private CharyparNagelScoringParametersForPerson params;
>>>>>>> master

    @Inject
    public AVScoringFunctionFactory(Scenario scenario, AVConfig config) {
<<<<<<< HEAD
		this.config = config;
        params = new SubpopulationScoringParameters(scenario);
        standardFactory = new CharyparNagelScoringFunctionFactory(scenario);
=======
	this.config = config;
	params = new SubpopulationCharyparNagelScoringParameters(scenario);
	standardFactory = new CharyparNagelScoringFunctionFactory(scenario);
>>>>>>> master
    }

    @Override
    public ScoringFunction createNewScoringFunction(Person person) {
	SumScoringFunction sf = (SumScoringFunction) standardFactory.createNewScoringFunction(person);

	double marginalUtilityOfMoney = params.getScoringParameters(person).marginalUtilityOfMoney;
	double marginalUtilityOfTraveling = params.getScoringParameters(person).modeParams
		.get(AVModule.AV_MODE).marginalUtilityOfTraveling_s;

	sf.addScoringFunction(
		new AVScoringFunction(config, person, marginalUtilityOfMoney, marginalUtilityOfTraveling));

	return sf;
    }
}
