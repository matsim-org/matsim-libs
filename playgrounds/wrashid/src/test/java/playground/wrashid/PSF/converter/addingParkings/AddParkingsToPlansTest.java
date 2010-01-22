package playground.wrashid.PSF.converter.addingParkings;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.testcases.MatsimTestCase;

public class AddParkingsToPlansTest extends MatsimTestCase {

	public void testGeneratePlanWithParkingActs(){
		String basePathOfTestData=getPackageInputDirectory();
		String networkFile = "test/scenarios/berlin/network.xml.gz";
		
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		new MatsimPopulationReader(scenario).readFile(basePathOfTestData + "plans5.xml");
		
		// generate parking facilities
		GenerateParkingFacilities.generateParkingFacilties(scenario);

		// generate plans with parking
		AddParkingsToPlans.generatePlanWithParkingActs(scenario);
		
		Population population = scenario.getPopulation();
		
		// check, the population size 
		assertEquals(3, population.getPersons().size());
		
		// check number of acts and legs (for one agent)
		Person person=population.getPersons().get(new IdImpl("66128"));
		assertEquals(13, person.getSelectedPlan().getPlanElements().size());
		
		// check that departing activity from home parking is there
		assertEquals("parkingDeparture", ((Activity) person.getSelectedPlan().getPlanElements().get(2)).getType());
		
		// check that arrival activity at home parking is there
		assertEquals("parkingArrival", ((Activity) person.getSelectedPlan().getPlanElements().get(10)).getType());
		
		
		// check, that the parking activities have the right linkId assigned
		assertEquals("1921", ((Activity) person.getSelectedPlan().getPlanElements().get(2)).getLinkId().toString());
		
		assertEquals("13816", ((Activity) person.getSelectedPlan().getPlanElements().get(4)).getLinkId().toString());
		// TODO: the facility Ids seem to be missing...
		
		// check, that the agent with walk legs did not convert them to additional legs with parkings...
		person=population.getPersons().get(new IdImpl("1"));
		assertEquals(9, person.getSelectedPlan().getPlanElements().size());
		
	} 
	
}  
 