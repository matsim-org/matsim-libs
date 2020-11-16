package playground.vsp.scoring;

import org.junit.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

import static playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters.*;

/**
 * this class tests {@link IncomeDependentUtilityOfMoneyPersonScoringParameters}
 *
 * It checks whether the person specific income is read from the person attributes.
 * The marginalUtilityOfMoney should be calculated as averageIncome/personSpecificIncome and not taken from the subpopulation-specific scoring params.
 * To check whether the remaining scoring params are subpopulation-specific, this class tests the the person's marginalUtilityOfWaitingPt_s accordingly.
 *
 */
public class IncomeDependentUtilityOfMoneyPersonScoringParametersTest {

	@Rule
	public MatsimTestUtils utils;
	private static TransitConfigGroup transitConfigGroup;
	private static ScenarioConfigGroup scenarioConfigGroup;
	private static PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	private static IncomeDependentUtilityOfMoneyPersonScoringParameters personScoringParams;
	private static Population population;

	@BeforeClass
	public static void setUp() throws Exception {
		transitConfigGroup = new TransitConfigGroup();
		scenarioConfigGroup = new ScenarioConfigGroup();
		planCalcScoreConfigGroup = new PlanCalcScoreConfigGroup();

		PlanCalcScoreConfigGroup.ScoringParameterSet personParams = planCalcScoreConfigGroup.getOrCreateScoringParameters("person");
		personParams.setMarginalUtilityOfMoney(5000);
		personParams.setMarginalUtlOfWaitingPt_utils_hr(0.5 * 3600);

		PlanCalcScoreConfigGroup.ScoringParameterSet freightParams = planCalcScoreConfigGroup.getOrCreateScoringParameters("freight");
		freightParams.setMarginalUtilityOfMoney(444);
		freightParams.setMarginalUtlOfWaitingPt_utils_hr(1d * 3600);

		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory factory = population.getFactory();

		{ //fill population
			Person lowIncome = factory.createPerson(Id.createPersonId("lowIncome"));
			PopulationUtils.putSubpopulation(lowIncome, "person");
			PopulationUtils.putPersonAttribute(lowIncome, PERSONAL_INCOME_ATTRIBUTE_NAME, 0.5d);
			population.addPerson(lowIncome);

			Person mediumIncome = factory.createPerson(Id.createPersonId("mediumIncome"));
			PopulationUtils.putSubpopulation(mediumIncome, "person");
			PopulationUtils.putPersonAttribute(mediumIncome, PERSONAL_INCOME_ATTRIBUTE_NAME, 1d);
			population.addPerson(mediumIncome);

			Person highIncome = factory.createPerson(Id.createPersonId("highIncome"));
			PopulationUtils.putSubpopulation(highIncome, "person");
			PopulationUtils.putPersonAttribute(highIncome, PERSONAL_INCOME_ATTRIBUTE_NAME, 1.5d);
			population.addPerson(highIncome);

			Person freight = factory.createPerson(Id.createPersonId("freight"));
			PopulationUtils.putSubpopulation(freight, "freight");
			population.addPerson(freight);

			//a person living in a specifically poor are - average income is provided in person's attribute and might differ from global average
			Person mediumIncomeWithSpecificAverage = factory.createPerson(Id.createPersonId("mediumIncomeWithSpecificAverage"));
			PopulationUtils.putSubpopulation(mediumIncomeWithSpecificAverage, "person");
			PopulationUtils.putPersonAttribute(mediumIncomeWithSpecificAverage, PERSONAL_INCOME_ATTRIBUTE_NAME, 1d);
			PopulationUtils.putPersonAttribute(mediumIncomeWithSpecificAverage, INCOME_AVG_RELEVANT_FOR_PERSON_ATTRIBUTE_NAME, 0.1d);
			population.addPerson(mediumIncomeWithSpecificAverage);
		}
		personScoringParams = new IncomeDependentUtilityOfMoneyPersonScoringParameters(population, planCalcScoreConfigGroup, scenarioConfigGroup, transitConfigGroup);
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
	public void testPersonFreight(){
		Id<Person> id = Id.createPersonId("freight");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//freight agent has no income attribute set, so it should use the marginal utility of money that is set in it's subpopulation scoring parameters!
		makeAssert(params, 1d/444d, 1d);
	}

	@Test
	public void testPersonWithSpecificIncomeAverage(){
		Id<Person> id = Id.createPersonId("mediumIncomeWithSpecificAverage");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//this agent actually has income attribute set to 1, but it's person-specific average income attribute is set to 0.1.
		// the resulting marginal utility of money should thus be 1/10 which is the same as if it used the global average income (1) and had an income of 10.
		makeAssert(params, 10d, 0.5d);
	}

	@Test
	public void testMoneyScore(){
		ScoringParameters paramsRich = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("highIncome")));
		CharyparNagelMoneyScoring moneyScoringRich = new CharyparNagelMoneyScoring(paramsRich);
		moneyScoringRich.addMoney(100);
		Assert.assertEquals("for the rich person, 100 money units should be equal to a score of 66.66", 1./1.5 * 100, moneyScoringRich.getScore(), utils.EPSILON);

		ScoringParameters paramsPoor = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("lowIncome")));
		CharyparNagelMoneyScoring moneyScoringPoor = new CharyparNagelMoneyScoring(paramsPoor);
		moneyScoringPoor.addMoney(100);
		Assert.assertEquals("for the poor person, 100 money units should be equal to a score of 200.00", 1./0.5 * 100, moneyScoringPoor.getScore(), utils.EPSILON);

		Assert.assertTrue("100 money units should worth more for a poor person than for a rich person", moneyScoringPoor.getScore() > moneyScoringRich.getScore());
	}

	private void makeAssert(ScoringParameters params, double income, double marginalUtilityOfWaitingPt_s){
		Assert.assertEquals("marginalUtilityOfMoney is wrong", 1 / income , params.marginalUtilityOfMoney, 0.);
		Assert.assertEquals("marginalUtilityOfWaitingPt_s is wrong", marginalUtilityOfWaitingPt_s , params.marginalUtilityOfWaitingPt_s, 0.);
	}


}