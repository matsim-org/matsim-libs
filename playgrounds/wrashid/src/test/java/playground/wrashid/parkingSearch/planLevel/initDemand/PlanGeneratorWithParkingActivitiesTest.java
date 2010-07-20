package playground.wrashid.parkingSearch.planLevel.initDemand;

import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import junit.framework.TestCase;

public class PlanGeneratorWithParkingActivitiesTest extends TestCase{

	public void testBasic(){
		
		String inputPlansFilePath = "test/scenarios/chessboard/plans.xml";
		String networkFilePath = "test/scenarios/chessboard/network.xml";
		String facilitiesFilePath = "test/input/playground/wrashid/parkingSearch/planLevel/chessFacilities.xml";
		
		PlanGeneratorWithParkingActivities pghc=new PlanGeneratorWithParkingActivities(inputPlansFilePath, networkFilePath, facilitiesFilePath);
		
		pghc.processPlans();
		
		ScenarioImpl scenario = pghc.getScenario();
		
		Plan planOfPersonOne=scenario.getPopulation().getPersons().get(new IdImpl(1)).getSelectedPlan();	
		
		List<PlanElement> planElements=planOfPersonOne.getPlanElements();
		
		assertEquals(19, planElements.size());
		
		// assert, that plan elements switch between act and leg.
		for (int i=0;i<planElements.size();i++){
			if (i % 2==0){
				assertTrue(planElements.get(i) instanceof ActivityImpl);
			} else {
				assertTrue(planElements.get(i) instanceof LegImpl);
			}
		}
	}
	
}
