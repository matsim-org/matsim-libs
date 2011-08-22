package playground.jbischoff.matsimha2;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;

public class ChangeActivityTimesAdvancedTest {

	private ChangeActivityTimesAdvanced cata;
	private Plan testplan;
	private Id id1;

	
	
	public ChangeActivityTimesAdvancedTest() {
		
		try {
			this.setUp();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Before
	public void setUp() throws Exception {
	this.cata = new ChangeActivityTimesAdvanced();
	
	Config config = ConfigUtils.createConfig();
	Scenario sc = ScenarioUtils.createScenario(config);

	this.id1 = sc.createId("1");

	Population population = sc.getPopulation();   
	PopulationFactory populationFactory = population.getFactory();

	Person person = populationFactory.createPerson(id1);
	population.addPerson(person);


	testplan = populationFactory.createPlan();
	person.addPlan(testplan);


	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

	Coord homeCoordinates = sc.createCoord(13.077, 52.357);
	Activity activity1 = populationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
	activity1.setEndTime(8*3600);
	testplan.addActivity(activity1);


	testplan.addLeg(populationFactory.createLeg("car"));


	Activity activity2 = populationFactory.createActivityFromCoord("work", ct.transform(sc.createCoord(13.0500, 52.441)));
	activity2.setEndTime(16*3600);
	testplan.addActivity(activity2);


	testplan.addLeg(populationFactory.createLeg("car"));


	Activity activity3 = populationFactory.createActivityFromCoord("home", ct.transform(homeCoordinates));
	testplan.addActivity(activity3);

	
	}
	
		
	
	@Test
	public void testHandlePlanAndHandleEvent() {
		
		ActivityStartEvent ev = new ActivityStartEventImpl(8.5*3600, id1, new IdImpl("3"), new IdImpl("4"),"work");
		this.cata.handleEvent(ev);
		Assert.assertEquals(8.5*3600, this.cata.getLastWorkActivityStartTime(new IdImpl("1")),0.0);
		
		
		this.cata.handlePlan(testplan);
		Activity act1 = (Activity)testplan.getPlanElements().get(0);
		Assert.assertEquals(8.0*3600,act1.getEndTime(),3600.0);

	}


	

}
