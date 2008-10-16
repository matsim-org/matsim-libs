package org.matsim.scoring;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

public class CharyparNagelOpenTimesScoringFunctionTest extends MatsimTestCase {

	private Person person = null;
	private Plan plan = null;

	private static final String UNUSED_OPENTIME_ACTIVITY_TYPE = "no wed and wkday open time activity";
	private static final String ONE_WKDAY_ACTIVITY_TYPE = "one opening interval on wkday activity";
	private static final String TWO_WEDNESDAY_ACTIVITY_TYPE = "two opening intervals on wednesday activity";

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// create facilities, activities in it and open times
		Facilities facilities = new Facilities();
		Gbl.getWorld().setFacilityLayer(facilities);

		Coord defaultCoord = new CoordImpl(0.0, 0.0);
		Facility testFacility = facilities.createFacility(new IdImpl(0), defaultCoord);

		Activity noWedAndWkDay = testFacility.createActivity(CharyparNagelOpenTimesScoringFunctionTest.UNUSED_OPENTIME_ACTIVITY_TYPE);
		noWedAndWkDay.createOpentime("fri", 8.0 * 3600, 16.0 * 3600);

		Activity wkdayActivity = testFacility.createActivity(CharyparNagelOpenTimesScoringFunctionTest.ONE_WKDAY_ACTIVITY_TYPE);
		wkdayActivity.createOpentime("wkday", 7.5 * 3600, 18.0 * 3600);

		Activity wednesdayActivity = testFacility.createActivity(CharyparNagelOpenTimesScoringFunctionTest.TWO_WEDNESDAY_ACTIVITY_TYPE);
		wednesdayActivity.createOpentime("wed", 6.0 * 3600, 11.0 * 3600);
		wednesdayActivity.createOpentime("wed", 13.0 * 3600, 19.0 * 3600);
		// this one should be ignored
		wednesdayActivity.createOpentime("wkday", 4.0 * 3600, 20.0 * 3600);

		// here, we don't test the scoring function itself, but just the method to retrieve opening times
		// we don't really need persons and plans, they're just used to initialize the ScoringFunction object
		this.person = new Person(new IdImpl(1));
		this.plan = person.createPlan(true);

		Act act = plan.createAct("no type", testFacility);
		act.setStartTime(8.0 * 3600);
		act.setDur(8.0 * 3600);
		act.setEndTime(16.0 * 3600);
		act.setFacility(testFacility);
	}

	@Override
	protected void tearDown() throws Exception {
		this.plan = null;
		this.person = null;
		super.tearDown();
	}

	public void testGetOpeningInterval() throws Exception {

		Act act = this.plan.getFirstActivity();

		CharyparNagelOpenTimesScoringFunction.initialized = true;
		CharyparNagelOpenTimesScoringFunction testee = new CharyparNagelOpenTimesScoringFunction(this.plan);

		double[] openInterval = null;

		act.setType(CharyparNagelOpenTimesScoringFunctionTest.UNUSED_OPENTIME_ACTIVITY_TYPE);

		openInterval = testee.getOpeningInterval(act);

		assertEquals(openInterval[0], Time.UNDEFINED_TIME, EPSILON);
		assertEquals(openInterval[1], Time.UNDEFINED_TIME, EPSILON);

		act.setType(CharyparNagelOpenTimesScoringFunctionTest.ONE_WKDAY_ACTIVITY_TYPE);

		openInterval = testee.getOpeningInterval(act);

		assertEquals(openInterval[0], 7.5 * 3600, EPSILON);
		assertEquals(openInterval[1], 18.0 * 3600, EPSILON);

		act.setType(CharyparNagelOpenTimesScoringFunctionTest.TWO_WEDNESDAY_ACTIVITY_TYPE);

		openInterval = testee.getOpeningInterval(act);

		assertEquals(openInterval[0], 6.0 * 3600, EPSILON);
		assertEquals(openInterval[1], 19.0 * 3600, EPSILON);
	}

}
