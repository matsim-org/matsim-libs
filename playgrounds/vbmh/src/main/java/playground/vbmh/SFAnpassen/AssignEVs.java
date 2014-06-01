package playground.vbmh.SFAnpassen;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vbmh.vmEV.*;


public class AssignEVs {
	static Scenario scenario;
	static Random zufall = new Random();
	static EVListWriter writer = new EVListWriter();
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		EVList evList = new EVList();
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF_PLUS/config_SF_PLUS_3.xml"));
		String evOutputFile="input/SF_PLUS/VM/evs.xml";
		int ev_i=0;
		int ev_j=0;
		for(Person person : scenario.getPopulation().getPersons().values()){
			if(zufall.nextDouble()<probabilityOfEVOwnership(person)){
				EV ev = new EV();
				ev.setId(Integer.toString(ev_i));
				ev.setOwnerPersonId(person.getId().toString());
				ev.batteryCapacity=18.7;
				ev.consumptionPerHundredKlicks=11.7;
				ev.evType="w-Zo"; 
				evList.addEV(ev);
				System.out.println(ev_i);
				System.out.println(evList.getOwnerMap().size());
				ev_i++;
				
			}
			ev_j++;
			
		}
		System.out.println(evList.getOwnerMap().values().size());
		System.out.println("Anzahl Agents insgesammt :"+ev_j);
		writer.write(evList, evOutputFile);
	
			
	}
	
	static double probabilityOfEVOwnership(Person person){
		PersonImpl personImpl = (PersonImpl) person;
		return 0.2;
	}

}
