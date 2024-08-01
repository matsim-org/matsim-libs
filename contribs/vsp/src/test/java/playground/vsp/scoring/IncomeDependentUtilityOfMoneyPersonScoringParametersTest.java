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
 * To check whether the remaining scoring params are subpopulation-specific, this class tests the the person's marginalUtilityOfWaitingPt_s accordingly.
 *
 */
public class IncomeDependentUtilityOfMoneyPersonScoringParametersTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();
	private IncomeDependentUtilityOfMoneyPersonScoringParameters personScoringParams;
	private Population population;

	@BeforeEach
	public void setUp() {
		TransitConfigGroup transitConfigGroup = new TransitConfigGroup();
		ScenarioConfigGroup scenarioConfigGroup = new ScenarioConfigGroup();
		ScoringConfigGroup scoringConfigGroup = new ScoringConfigGroup();

		ScoringConfigGroup.ScoringParameterSet personParams = scoringConfigGroup.getOrCreateScoringParameters("person");
		personParams.setMarginalUtilityOfMoney(1);
		personParams.setMarginalUtlOfWaitingPt_utils_hr(0.5 * 3600);

		ScoringConfigGroup.ScoringParameterSet freightParams = scoringConfigGroup.getOrCreateScoringParameters("freight");
		freightParams.setMarginalUtilityOfMoney(444);
		freightParams.setMarginalUtlOfWaitingPt_utils_hr(1d * 3600);

		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory factory = population.getFactory();

		{ //fill population
			Person negativeIncome = factory.createPerson(Id.createPersonId("negativeIncome"));
			PopulationUtils.putSubpopulation(negativeIncome, "person");
			PersonUtils.setIncome(negativeIncome, -100d);
			population.addPerson(negativeIncome);

			Person zeroIncome = factory.createPerson(Id.createPersonId("zeroIncome"));
			PopulationUtils.putSubpopulation(zeroIncome, "person");
			PersonUtils.setIncome(zeroIncome, 0d);
			population.addPerson(zeroIncome);

			Person lowIncome = factory.createPerson(Id.createPersonId("lowIncome"));
			PopulationUtils.putSubpopulation(lowIncome, "person");
			PersonUtils.setIncome(lowIncome, 0.5d);
			population.addPerson(lowIncome);

			Person mediumIncome = factory.createPerson(Id.createPersonId("mediumIncome"));
			PopulationUtils.putSubpopulation(mediumIncome, "person");
			PersonUtils.setIncome(mediumIncome, 1d);
			population.addPerson(mediumIncome);

			Person highIncome = factory.createPerson(Id.createPersonId("highIncome"));
			PopulationUtils.putSubpopulation(highIncome, "person");
			PersonUtils.setIncome(highIncome, 1.5d);
			population.addPerson(highIncome);

			Person freight = factory.createPerson(Id.createPersonId("freight"));
			PopulationUtils.putSubpopulation(freight, "freight");
			population.addPerson(freight);

			Person freightWithIncome1 = factory.createPerson(Id.createPersonId("freightWithIncome1"));
			PopulationUtils.putSubpopulation(freightWithIncome1, "freight");
			PersonUtils.setIncome(freightWithIncome1, 1.5d);
			population.addPerson(freightWithIncome1);

			Person freightWithIncome2 = factory.createPerson(Id.createPersonId("freightWithIncome2"));
			PopulationUtils.putSubpopulation(freightWithIncome2, "freight");
			PersonUtils.setIncome(freightWithIncome2, 0.5d);
			population.addPerson(freightWithIncome2);
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
	void testPersonFreight(){
		Id<Person> id = Id.createPersonId("freight");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//freight agent has no income attribute set, so it should use the marginal utility of money that is set in it's subpopulation scoring parameters!
		makeAssert(params, 1d/444d, 1d);
	}

	@Test
	void testFreightWithIncome(){
		Id<Person> id = Id.createPersonId("freightWithIncome1");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssert(params, 1.5/444d, 1d);
		Id<Person> id2 = Id.createPersonId("freightWithIncome2");
		ScoringParameters params2 = personScoringParams.getScoringParameters(population.getPersons().get(id2));
		makeAssert(params2, 0.5/444d, 1d);
	}

	@Test
	void testMoneyScore(){
		ScoringParameters paramsRich = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("highIncome")));
		CharyparNagelMoneyScoring moneyScoringRich = new CharyparNagelMoneyScoring(paramsRich);
		moneyScoringRich.addMoney(100);
		Assertions.assertEquals(1./1.5 * 100, moneyScoringRich.getScore(), MatsimTestUtils.EPSILON, "for the rich person, 100 money units should be equal to a score of 66.66");

		ScoringParameters paramsPoor = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("lowIncome")));
		CharyparNagelMoneyScoring moneyScoringPoor = new CharyparNagelMoneyScoring(paramsPoor);
		moneyScoringPoor.addMoney(100);
		Assertions.assertEquals(1./0.5 * 100, moneyScoringPoor.getScore(), MatsimTestUtils.EPSILON, "for the poor person, 100 money units should be equal to a score of 200.00");

		Assertions.assertTrue(moneyScoringPoor.getScore() > moneyScoringRich.getScore(), "100 money units should worth more for a poor person than for a rich person");
	}

	private void makeAssert(ScoringParameters params, double income, double marginalUtilityOfWaitingPt_s){
		Assertions.assertEquals(1 / income , params.marginalUtilityOfMoney, 0., "marginalUtilityOfMoney is wrong");
		Assertions.assertEquals(marginalUtilityOfWaitingPt_s , params.marginalUtilityOfWaitingPt_s, 0., "marginalUtilityOfWaitingPt_s is wrong");
	}


}
