package playground.wrashid.parkingChoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import junit.framework.TestCase;

public class ParkingChoiceLibTest extends TestCase {

	public void testGetLastActivityInfo(){
		String basePath="test/scenarios/chessboard/";
		String plansFile=basePath + "plans.xml";
		String networkFile=basePath + "network.xml";
		String facilititiesPath=basePath + "facilities.xml";
		Scenario scenario = GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
	
		ActInfo lastActivityInfo = ParkingChoiceLib.getLastActivityInfo(scenario.getPopulation().getPersons().get(new IdImpl(1)).getSelectedPlan());
		
		assertEquals("1", lastActivityInfo.getFacilityId().toString());
		assertEquals("home", lastActivityInfo.getActType().toString());
		
		lastActivityInfo = ParkingChoiceLib.getLastActivityInfo(scenario.getPopulation().getPersons().get(new IdImpl(177)).getSelectedPlan());
		
		assertEquals("20", lastActivityInfo.getFacilityId().toString());
		assertEquals("home", lastActivityInfo.getActType().toString());
	}
	
}
