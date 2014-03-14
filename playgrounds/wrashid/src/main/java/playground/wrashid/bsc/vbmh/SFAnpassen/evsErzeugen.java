package playground.wrashid.bsc.vbmh.SFAnpassen;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.wrashid.bsc.vbmh.vmEV.*;


public class evsErzeugen {
	static Scenario scenario;
	static Random zufall = new Random();
	static EVListWriter writer = new EVListWriter();
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		EVList evList = new EVList();
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF/config_SF_3.xml"));
		int i=0;
		for(Person person : scenario.getPopulation().getPersons().values()){
			if(zufall.nextDouble()<probabilityOfEVOwnership(person)){
				EV ev = new EV();
				ev.setId(Integer.toString(i));
				ev.setOwnerPersonId(person.getId().toString());
				ev.batteryCapacity=1000;
				evList.addEV(ev);
				System.out.println(i);
				System.out.println(evList.getOwnerMap().size());
				i++;
				
			}
			
		}
		evList.getOwnerMap().get(3);
		System.out.println(evList.getOwnerMap().values().size());
		writer.write(evList, "input/evs_demo.xml");
	
			
	}
	
	static double probabilityOfEVOwnership(Person person){
		
		return 0.2;
	}

}
