package playground.wrashid.parkingChoice;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.parkingChoice.infrastructure.ActInfo;

public class ParkingChoiceLibTest extends TestCase {

	public void testGetLastActivity(){
		Scenario scenario = getScenario();
	
		ActInfo lastActivityInfo = ParkingChoiceLib.getLastActivityInfo(scenario.getPopulation().getPersons().get(Id.create(1, Person.class)).getSelectedPlan());
		
		assertEquals("1", lastActivityInfo.getFacilityId().toString());
		assertEquals("home", lastActivityInfo.getActType().toString());
		
		lastActivityInfo = ParkingChoiceLib.getLastActivityInfo(scenario.getPopulation().getPersons().get(Id.create(177, Person.class)).getSelectedPlan());
		
		assertEquals("20", lastActivityInfo.getFacilityId().toString());
		assertEquals("home", lastActivityInfo.getActType().toString());
	}

	public void testGetLastActivityPreecededByCardLeg(){
		Scenario scenario = getScenario();
	
		ActInfo lastActivityInfo = ParkingChoiceLib.getLastActivityInfoPreceededByCarLeg(scenario.getPopulation().getPersons().get(Id.create(1, Person.class)).getSelectedPlan());
		
		assertEquals("1", lastActivityInfo.getFacilityId().toString());
		assertEquals("home", lastActivityInfo.getActType().toString());
		
		lastActivityInfo = ParkingChoiceLib.getLastActivityInfoPreceededByCarLeg(scenario.getPopulation().getPersons().get(Id.create(177, Person.class)).getSelectedPlan());
		
		assertEquals("20", lastActivityInfo.getFacilityId().toString());
		assertEquals("home", lastActivityInfo.getActType().toString());
	}
	
	private Scenario getScenario() {
		String basePath="test/scenarios/chessboard/";
		String plansFile=basePath + "plans.xml";
		String networkFile=basePath + "network.xml";
		String facilititiesPath=basePath + "facilities.xml";
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		return scenario;
	}
	
}
