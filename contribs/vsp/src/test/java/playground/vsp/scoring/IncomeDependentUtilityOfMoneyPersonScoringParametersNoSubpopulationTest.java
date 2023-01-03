package playground.vsp.scoring;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
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

	@Rule
	public MatsimTestUtils utils;
	private IncomeDependentUtilityOfMoneyPersonScoringParameters personScoringParams;
	private Population population;

	@Before
	public void setUp() {
		TransitConfigGroup transitConfigGroup = new TransitConfigGroup();
		ScenarioConfigGroup scenarioConfigGroup = new ScenarioConfigGroup();
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = new PlanCalcScoreConfigGroup();

		PlanCalcScoreConfigGroup.ScoringParameterSet defaultParams = planCalcScoreConfigGroup.getOrCreateScoringParameters(null);
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
				planCalcScoreConfigGroup,
				scenarioConfigGroup,
				transitConfigGroup);
	}

	@Test
	public void testPersonWithNegativeIncome(){
		Id<Person> id = Id.createPersonId("negativeIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//person's attribute says it has negative income which is considered invalid and therefore the subpopulation's mgnUtilityOfMoney is taken (which is 1)
		makeAssert(params, 1d, 0.5d);
	}

	@Test
	public void testPersonWithNoIncome(){
		Id<Person> id = Id.createPersonId("zeroIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//person's attribute says it has 0 income which is considered invalid and therefore the subpopulation's mgnUtilityOfMoney is taken (which is 1)
		makeAssert(params, 1d, 0.5d);
	}

	@Test
	public void testPersonWithLowIncome(){
		Id<Person> id = Id.createPersonId("lowIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssert(params, 0.5d, 0.5d);
	}

	@Test
	public void testPersonWithHighIncome(){
		Id<Person> id = Id.createPersonId("highIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssert(params, 1.5d, 0.5d);
	}

	@Test
	public void testPersonWithMediumIncome(){
		Id<Person> id = Id.createPersonId("mediumIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssert(params, 1d, 0.5d);
	}

	@Test
	public void testMoneyScore(){
		ScoringParameters paramsRich = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("highIncome")));
		CharyparNagelMoneyScoring moneyScoringRich = new CharyparNagelMoneyScoring(paramsRich);
		moneyScoringRich.addMoney(100);
		Assert.assertEquals("for the rich person, 100 money units should be equal to a score of ", 20 * 1./1.5 * 100, moneyScoringRich.getScore(), MatsimTestUtils.EPSILON);

		ScoringParameters paramsPoor = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("lowIncome")));
		CharyparNagelMoneyScoring moneyScoringPoor = new CharyparNagelMoneyScoring(paramsPoor);
		moneyScoringPoor.addMoney(100);
		Assert.assertEquals("for the poor person, 100 money units should be equal to a score of ", 20 * 1./0.5 * 100, moneyScoringPoor.getScore(), MatsimTestUtils.EPSILON);

		Assert.assertTrue("100 money units should worth more for a poor person than for a rich person", moneyScoringPoor.getScore() > moneyScoringRich.getScore());
	}

	private void makeAssert(ScoringParameters params, double income, double marginalUtilityOfWaitingPt_s){
		Assert.assertEquals("marginalUtilityOfMoney is wrong", 20 * 1 / income , params.marginalUtilityOfMoney, 0.);
		Assert.assertEquals("marginalUtilityOfWaitingPt_s is wrong", marginalUtilityOfWaitingPt_s , params.marginalUtilityOfWaitingPt_s, 0.);
	}


}