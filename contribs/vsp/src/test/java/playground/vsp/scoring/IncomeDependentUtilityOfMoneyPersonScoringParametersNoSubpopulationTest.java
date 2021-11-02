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
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

import static playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME;

/**
 * this class tests {@link IncomeDependentUtilityOfMoneyPersonScoringParameters}
 *
 * It checks whether the person specific income is read from the person attributes.
 * The marginalUtilityOfMoney should be calculated as averageIncome/personSpecificIncome and not taken from the subpopulation-specific scoring params.
 * To check whether the remaining scoring params are subpopulation-specific, this class tests the the person's marginalUtilityOfWaitingPt_s accordingly.
 *
 */
public class IncomeDependentUtilityOfMoneyPersonScoringParametersNoSubpopulationTest {

	@Rule
	public MatsimTestUtils utils;
	private IncomeDependentUtilityOfMoneyPersonScoringParameters personScoringParams;
	private Population population;

	double averageIncome = 100;

	@Before
	public void setUp() {
		TransitConfigGroup transitConfigGroup = new TransitConfigGroup();
		ScenarioConfigGroup scenarioConfigGroup = new ScenarioConfigGroup();
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = new PlanCalcScoreConfigGroup();

		PlanCalcScoreConfigGroup.ScoringParameterSet defaultParams = planCalcScoreConfigGroup.getOrCreateScoringParameters(null);
		defaultParams.setMarginalUtilityOfMoney(20);
		defaultParams.setMarginalUtlOfWaitingPt_utils_hr(0.0 * 3600);

		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory factory = population.getFactory();

		{ 	Person noSubPopulationNoIncome = factory.createPerson(Id.createPersonId("noSubPopulationNoIncome"));
			population.addPerson(noSubPopulationNoIncome);

			Person lowIncomeNoSubpopulation = factory.createPerson(Id.createPersonId("lowIncomeNoSubPopulation"));
			PopulationUtils.putPersonAttribute(lowIncomeNoSubpopulation, PERSONAL_INCOME_ATTRIBUTE_NAME, 0.5d * averageIncome);
			population.addPerson(lowIncomeNoSubpopulation);

			Person highIncomeNoSubpopulation = factory.createPerson(Id.createPersonId("highIncomeNoSubPopulation"));
			PopulationUtils.putPersonAttribute(highIncomeNoSubpopulation, PERSONAL_INCOME_ATTRIBUTE_NAME, 1.5d * averageIncome);
			population.addPerson(highIncomeNoSubpopulation);

		}
		personScoringParams = new IncomeDependentUtilityOfMoneyPersonScoringParameters(population,
				planCalcScoreConfigGroup,
				scenarioConfigGroup,
				transitConfigGroup);
	}

	@Test
	public void testPersonWithNoSubpopulationButLowIncome(){
		Id<Person> id = Id.createPersonId("lowIncomeNoSubPopulation");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssert(params,  0.5d * averageIncome, 0);
	}

	@Test
	public void testPersonWithNoSubpopulationButHighIncome(){
		Id<Person> id = Id.createPersonId("highIncomeNoSubPopulation");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssert(params, 1.5d * averageIncome, 0);
	}

	@Test
	public void testPersonWithNoSubpopulationAndNoIncome(){
		Id<Person> id = Id.createPersonId("noSubPopulationNoIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//person's attribute says it has negative income which is considered invalid and therefore the subpopulation's mgnUtilityOfMoney is taken (which is 111)
		makeAssert(params, averageIncome, 0);
	}

	private void makeAssert(ScoringParameters params, double income, double marginalUtilityOfWaitingPt_s){
		Assert.assertEquals("marginalUtilityOfMoney is wrong", 20 * averageIncome / income , params.marginalUtilityOfMoney, 0.);
		Assert.assertEquals("marginalUtilityOfWaitingPt_s is wrong", marginalUtilityOfWaitingPt_s , params.marginalUtilityOfWaitingPt_s, 0.);
	}


}