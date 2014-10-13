package playground.wrashid.lib.tools.plan;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.population.PersonImpl;


/**
 * TODO: tidy up this class a bit!
 * @author wrashid
 *
 */
public class PlanCloner {

	public static void main(String[] args) {
		
		// input parameters
		String inputPlansFile="C:/data/workspace/playgrounds/wrashid/test/input/playground/wrashid/parkingSearch/planLevel/chessPlans2.xml";
		String inputNetworkFile="C:/data/workspace/playgrounds/wrashid/test/scenarios/chessboard/network.xml";
		String outputPlansFile="C:/data/workspace/playgrounds/wrashid/test/input/playground/wrashid/parkingSearch/planLevel/chessPlanxxxx.xml";
		int numberOfClones=100;	
		String idOfPersonForCloning="1";
		
		// start program
		
		Scenario scenario= GeneralLib.readScenario(inputPlansFile, inputNetworkFile);
		
		Person selectedPersonForCloning=scenario.getPopulation().getPersons().get(Id.create(idOfPersonForCloning, Person.class));
		
		scenario.getPopulation().getPersons().clear();
		
		for (int i=1;i<=numberOfClones;i++){
			Person person=GeneralLib.copyPerson(selectedPersonForCloning);
            ((PersonImpl) person).setId(Id.create(i, Person.class));
            scenario.getPopulation().addPerson(person);
		}
		
		GeneralLib.writePersons(scenario.getPopulation().getPersons().values(), outputPlansFile, scenario.getNetwork());
	}
	
	

	
}
