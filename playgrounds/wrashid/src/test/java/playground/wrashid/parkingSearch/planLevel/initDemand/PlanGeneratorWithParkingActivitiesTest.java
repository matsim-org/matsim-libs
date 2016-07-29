package playground.wrashid.parkingSearch.planLevel.initDemand;

import java.util.List;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scenario.MutableScenario;

public class PlanGeneratorWithParkingActivitiesTest extends TestCase{

	public void testBasic(){
		
		String inputPlansFilePath = "test/scenarios/chessboard/plans.xml";
		String networkFilePath = "test/scenarios/chessboard/network.xml";
		String facilitiesFilePath = "test/input/playground/wrashid/parkingSearch/planLevel/chessFacilities.xml";
		
		PlanGeneratorWithParkingActivities pghc=new PlanGeneratorWithParkingActivities(inputPlansFilePath, networkFilePath, facilitiesFilePath);
		
		pghc.processPlans();
		
		MutableScenario scenario = pghc.getScenario();
		
		Plan planOfPersonOne=scenario.getPopulation().getPersons().get(Id.create(1, Person.class)).getSelectedPlan();	
		
		List<PlanElement> planElements=planOfPersonOne.getPlanElements();
		
		assertEquals(19, planElements.size());
		
		// assert, that plan elements switch between act and leg.
		for (int i=0;i<planElements.size();i++){
			if (i % 2==0){
				assertTrue(planElements.get(i) instanceof Activity);
			} else {
				assertTrue(planElements.get(i) instanceof Leg);
			}
		}
	}
	
}
