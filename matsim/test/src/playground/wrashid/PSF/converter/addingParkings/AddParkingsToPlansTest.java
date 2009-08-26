package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.lib.GeneralLib;

public class AddParkingsToPlansTest extends MatsimTestCase {

	public void testGeneratePlanWithParkingActs(){
		String basePathOfTestData="test/input/playground/wrashid/PSF/converter/addParkings/";
		String networkFile = "test/scenarios/berlin/network.xml.gz";
		
		
		// generate plans with parking
		AddParkingsToPlans.generatePlanWithParkingActs(basePathOfTestData + "plans5.xml", networkFile, "output/plans.xml");
		
		Population population=GeneralLib.readPopulation("output/plans.xml", networkFile);
		
		// check, the population size 
		assertEquals(3, population.getPersons().size());
		
		// check number of acts and legs (for one agent)
		Person person=population.getPersons().get(new IdImpl("66128"));
		assertEquals(13, person.getSelectedPlan().getPlanElements().size());
		
		// check that departing activity from home parking is there
		assertEquals("parkingDeparture", ((Activity) person.getSelectedPlan().getPlanElements().get(2)).getType());
		
		// check that arrival activity at home parking is there
		assertEquals("parkingArrival", ((Activity) person.getSelectedPlan().getPlanElements().get(10)).getType());
		
		
		// TODO: check, that the right facilities, linkIds are take for the parking facilities and the legs have the
		// right time...
		
		//assertEquals(true, false);
		
		// check, that the agent with walk legs did not convert them to additional legs with parkings...
		person=population.getPersons().get(new IdImpl("1"));
		assertEquals(9, person.getSelectedPlan().getPlanElements().size());
		
		
	}
	
}
