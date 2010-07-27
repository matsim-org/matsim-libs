package playground.mfeil;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;

import playground.mfeil.FilesForTests.Initializer;
import playground.mfeil.MDSAM.ActivityTypeFinder;

public class RecyclingModuleTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(PlanomatXTest.class);
	private Initializer initializer;
	final String TEST_PERSON_ID = "10";
	private RecyclingModule testee;
	private ScenarioImpl scenario_input;

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		this.initializer = new Initializer();
		this.initializer.init(this);
		this.initializer.run();

		this.scenario_input = this.initializer.getControler().getScenario();

		// no events are used, hence an empty road network
		ActivityTypeFinder finder = new ActivityTypeFinder (this.initializer.getControler());

		this.testee = new RecyclingModule (this.initializer.getControler(), finder);
	}
	
	public void testAll (){
		this.testee.prepareReplanning();
		for (Person person : this.scenario_input.getPopulation().getPersons().values()){
			if (person.getId().toString().equals("1")||
					person.getId().toString().equals("2")||
					person.getId().toString().equals("3")||
					person.getId().toString().equals("4")||
					person.getId().toString().equals("5"))continue;
			this.testee.handlePlan(person.getSelectedPlan());
		}
		this.testee.finishReplanning();
		
		// Compare the two plans; <1 because of double rounding errors
		assertEquals(this.testee.coefficients.getSingleCoef("primActsDistance"),0.0);
		assertEquals(this.testee.coefficients.getSingleCoef("homeLocationDistance"),2.5);
		assertEquals(this.testee.coefficients.getSingleCoef("age"),1.0);
	}

}
