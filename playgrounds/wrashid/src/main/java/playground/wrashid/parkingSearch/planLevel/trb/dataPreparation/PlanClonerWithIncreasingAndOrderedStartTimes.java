package playground.wrashid.parkingSearch.planLevel.trb.dataPreparation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.PersonImpl;


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
		String outputPlansFile="H:/data/experiments/STRC2010/input/plans8.xml.gz";
		int numberOfClones=1000;	
		String idOfPersonForCloning="1";
		
		// start program
		
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile);
		
		Person selectedPersonForCloning=scenario.getPopulation().getPersons().get(Id.create(idOfPersonForCloning, Person.class));
		
		scenario.getPopulation().getPersons().clear();
		
		for (int i=1;i<=numberOfClones;i++){
			Person person=GeneralLib.copyPerson(selectedPersonForCloning);
            ((PersonImpl) person).setId(Id.create(i, Person.class));
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
		
		GeneralLib.writePersons(scenario.getPopulation().getPersons().values(), outputPlansFile, scenario.getNetwork());
	}
	
	

	
}
