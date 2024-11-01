package playground.vsp.scoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

/**
 * this class tests {@link IncomeDependentUtilityOfMoneyPersonScoringParameters}
 *
 * It checks whether the person specific income is read from the person attributes.
 * The marginalUtilityOfMoney should be calculated as averageIncome/personSpecificIncome and not taken from the subpopulation-specific scoring params.
 * To check whether the remaining scoring params are subpopulation-specific, this class tests the person's marginalUtilityOfWaitingPt_s accordingly.
 *
 */
public class IncomeDependentUtilityOfMoneyPersonScoringParametersNoSubpopulationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();
	private IncomeDependentUtilityOfMoneyPersonScoringParameters personScoringParams;
	private Population population;

	@BeforeEach
	public void setUp() {
		TransitConfigGroup transitConfigGroup = new TransitConfigGroup();
		ScenarioConfigGroup scenarioConfigGroup = new ScenarioConfigGroup();
		ScoringConfigGroup scoringConfigGroup = new ScoringConfigGroup();

		ScoringConfigGroup.ScoringParameterSet defaultParams = scoringConfigGroup.getOrCreateScoringParameters(null);
		defaultParams.setMarginalUtilityOfMoney(20);
		defaultParams.setMarginalUtlOfWaitingPt_utils_hr(0.5d * 3600);

		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory factory = population.getFactory();

		{ //fill population
			Person negativeIncome = factory.createPerson(Id.createPersonId("negativeIncome"));
			PersonUtils.setIncome(negativeIncome, -100d);
			population.addPerson(negativeIncome);

			Person zeroIncome = factory.createPerson(Id.createPersonId("zeroIncome"));
			PersonUtils.setIncome(zeroIncome, 0d);
			population.addPerson(zeroIncome);

			Person lowIncome = factory.createPerson(Id.createPersonId("lowIncome"));
			PersonUtils.setIncome(lowIncome, 0.5d);
			population.addPerson(lowIncome);

			Person mediumIncome = factory.createPerson(Id.createPersonId("mediumIncome"));
			PersonUtils.setIncome(mediumIncome, 1d);
			population.addPerson(mediumIncome);

			Person highIncome = factory.createPerson(Id.createPersonId("highIncome"));
			PersonUtils.setIncome(highIncome, 1.5d);
			population.addPerson(highIncome);

		}
		personScoringParams = new IncomeDependentUtilityOfMoneyPersonScoringParameters(population,
			scoringConfigGroup,
				scenarioConfigGroup,
				transitConfigGroup);
	}

	@Test
	void testPersonWithNegativeIncome(){
		Id<Person> id = Id.createPersonId("negativeIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//person's attribute says it has negative income which is considered invalid and therefore the subpopulation's mgnUtilityOfMoney is taken (which is 1)
		makeAssert(params, 1d, 0.5d);
	}

	@Test
	void testPersonWithNoIncome(){
		Id<Person> id = Id.createPersonId("zeroIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//person's attribute says it has 0 income which is considered invalid and therefore the subpopulation's mgnUtilityOfMoney is taken (which is 1)
		makeAssert(params, 1d, 0.5d);
	}

	@Test
	void testPersonWithLowIncome(){
		Id<Person> id = Id.createPersonId("lowIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssert(params, 0.5d, 0.5d);
	}

	@Test
	void testPersonWithHighIncome(){
		Id<Person> id = Id.createPersonId("highIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssert(params, 1.5d, 0.5d);
	}

	@Test
	void testPersonWithMediumIncome(){
		Id<Person> id = Id.createPersonId("mediumIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssert(params, 1d, 0.5d);
	}

	@Test
	void testMoneyScore(){
		ScoringParameters paramsRich = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("highIncome")));
		CharyparNagelMoneyScoring moneyScoringRich = new CharyparNagelMoneyScoring(paramsRich);
		moneyScoringRich.addMoney(100);
		Assertions.assertEquals(20 * 1./1.5 * 100, moneyScoringRich.getScore(), MatsimTestUtils.EPSILON, "for the rich person, 100 money units should be equal to a score of ");

		ScoringParameters paramsPoor = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("lowIncome")));
		CharyparNagelMoneyScoring moneyScoringPoor = new CharyparNagelMoneyScoring(paramsPoor);
		moneyScoringPoor.addMoney(100);
		Assertions.assertEquals(20 * 1./0.5 * 100, moneyScoringPoor.getScore(), MatsimTestUtils.EPSILON, "for the poor person, 100 money units should be equal to a score of ");

		Assertions.assertTrue(moneyScoringPoor.getScore() > moneyScoringRich.getScore(), "100 money units should worth more for a poor person than for a rich person");
	}

	private void makeAssert(ScoringParameters params, double income, double marginalUtilityOfWaitingPt_s){
		Assertions.assertEquals(20 * 1 / income , params.marginalUtilityOfMoney, 0., "marginalUtilityOfMoney is wrong");
		Assertions.assertEquals(marginalUtilityOfWaitingPt_s , params.marginalUtilityOfWaitingPt_s, 0., "marginalUtilityOfWaitingPt_s is wrong");
	}


}
