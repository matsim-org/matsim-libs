package playground.balac.allcsmodestest.utils;

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;


public class TWUsersWithCar {

	public void run(String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[1]);
		populationReader.readFile(args[0]);		
		
		Set<Id> hasCar = new TreeSet<Id>();		
		int countCar = 0;
		int countLicence = 0;
		int countYoung = 0;
		
		for (Person person: scenario.getPopulation().getPersons().values()) {
			if (PersonUtils.getAge(person) >= 18) {
			if (!PersonUtils.getCarAvail(person).equals("never")  && PersonUtils.hasLicense(person))
				countCar++;
			
			if (PersonUtils.hasLicense(person))
				countLicence++;
			for (PlanElement pe:person.getSelectedPlan().getPlanElements()) {
				 if (pe instanceof Leg) {
					
					if (((Leg) pe).getMode().equals("twowaycarsharing")) {
						
						if (!PersonUtils.getCarAvail(person).equals("never")) {
							
							hasCar.add(person.getId());
						}				
					}
				 }			
			}
			}
			else
				countYoung++;
		}
	
		System.out.println(hasCar.size());
		System.out.println(countCar);
		System.out.println(countLicence);
		System.out.println(countYoung);



	}
	public static void main(String[] args) {

		TWUsersWithCar tWUsersWithCar = new TWUsersWithCar();
		tWUsersWithCar.run(args);
		
		
	}

}
