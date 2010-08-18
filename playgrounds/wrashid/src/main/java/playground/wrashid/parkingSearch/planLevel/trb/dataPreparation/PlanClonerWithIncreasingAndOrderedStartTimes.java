package playground.wrashid.parkingSearch.planLevel.trb.dataPreparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;

import playground.wrashid.lib.GeneralLib;

/**
 * TODO: tidy up this class a bit!
 * @author wrashid
 *
 */
public class PlanClonerWithIncreasingAndOrderedStartTimes {

	public static void main(String[] args) {
		
		// input parameters
		String inputPlansFile="C:/data/workspace/playgrounds/wrashid/test/input/playground/wrashid/parkingSearch/planLevel/chessPlans2.xml";
		String inputNetworkFile="C:/data/workspace/playgrounds/wrashid/test/scenarios/chessboard/network.xml";
		String outputPlansFile="C:/data/workspace/playgrounds/wrashid/test/input/playground/wrashid/parkingSearch/planLevel/chessPlans6.xml";
		int numberOfClones=100;	
		String idOfPersonForCloning="1";
		
		// start program
		
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile);
		
		Person selectedPersonForCloning=scenario.getPopulation().getPersons().get(new IdImpl(idOfPersonForCloning));
		
		scenario.getPopulation().getPersons().clear();
		
		for (int i=1;i<=numberOfClones;i++){
			Person person=GeneralLib.copyPerson(selectedPersonForCloning);
			person.setId(new IdImpl(i));
			Activity firstActivity=((Activity)person.getSelectedPlan().getPlanElements().get(0));
			double endTime=firstActivity.getEndTime();
			// the departure time of the agents is ordered in increasing order
			firstActivity.setEndTime(endTime+i*60);
			
			Activity workActivity=((Activity)person.getSelectedPlan().getPlanElements().get(6));
			endTime=workActivity.getEndTime();
			// the departure time of the agents is ordered in increasing order
			workActivity.setEndTime(endTime+i*60);
			
			
			scenario.getPopulation().addPerson(person);
		}
		
		GeneralLib.writePersons(scenario.getPopulation().getPersons().values(), outputPlansFile, (NetworkLayer) scenario.getNetwork());
	}
	
	

	
}
