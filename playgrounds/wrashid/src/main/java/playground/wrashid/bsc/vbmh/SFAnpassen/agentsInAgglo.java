package playground.wrashid.bsc.vbmh.SFAnpassen;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Household;

import playground.wrashid.PHEV.parking.data.Facility;

public class agentsInAgglo {

	static HashMap <Id, LinkedList <Person>> homes = new HashMap <Id, LinkedList <Person>>();
	static Scenario scenario;
	static double xCoord = 678773;
	static double yCoord = 4908813;
	static CoordImpl brookings = new CoordImpl(xCoord, yCoord);
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		double xCoord = 678773;
		double yCoord = 4908813;
		CoordImpl brookings = new CoordImpl(xCoord, yCoord);
		
		
		String outputFileF = "input/SF_PLUS/VM/facilities_brookings.xml";
		String outputFileP = "input/SF_PLUS/VM/population_brookings.xml";
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("input/SF_PLUS/config_SF_PLUS_2.xml"));
		
		
		
		int countAgents = 0;
		
	
		for(ActivityFacility homeFacility : scenario.getActivityFacilities().getFacilitiesForActivityType("home").values()){
			
			homes.put(homeFacility.getId(), new LinkedList<Person>());
				
			}
		
		
		for (Person person : scenario.getPopulation().getPersons().values()){
			Activity homeact = (Activity) person.getSelectedPlan().getPlanElements().iterator().next();
			if(homeact.getType() != "home"){
				System.out.println("Agent startet nicht zu hause");
			}
			
			if(homes.get(homeact.getFacilityId())!= null){
				homes.get(homeact.getFacilityId()).add(person);
			}
			
			
			
		}
		
		Random zufall = new Random();
		
		for(ActivityFacility homeFacility : scenario.getActivityFacilities().getFacilitiesForActivityType("home").values()){
			
			countAgents+=homes.get(homeFacility.getId()).size();
			System.out.println(homes.get(homeFacility.getId()).size());
			
			if(zufall.nextDouble()<0.3){
				move(homeFacility.getId());
			}
			
			
			
			
		}
		
		System.out.println(countAgents);
		
		
		
		
		
		
		PopulationWriter pwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		FacilitiesWriter fwriter = new FacilitiesWriter(scenario.getActivityFacilities());
		
		pwriter.writeV5(outputFileP);
		fwriter.write(outputFileF);
		
		System.out.println("Achtung: Ueberschreibt nicht richtig; Ausgabedatei sollte vorher geloescht werden.");
		
		

	}
	
	static void move(Id facId){
		IdImpl newFacId = new IdImpl(facId.toString()+"_B");
		for (Person person : homes.get(facId)){
			PersonImpl personImpl = (PersonImpl) person;
			for(PlanElement planElement : personImpl.getSelectedPlan().getPlanElements()){
				if(planElement.getClass().equals(ActivityImpl.class)){
					System.out.println("Activity gefunden");
					ActivityImpl activity = (ActivityImpl) planElement;
					if (activity.getType().equals("home")){
						activity.getCoord().setXY(xCoord, yCoord);
						
						activity.setFacilityId(newFacId);
						System.out.println("moved agent");
					}
				}
				
				
			}
		}
		
	
		ActivityFacility newCreatedFac = scenario.getActivityFacilities().getFactory().createActivityFacility(newFacId, brookings);
		scenario.getActivityFacilities().addActivityFacility(newCreatedFac);
		for (ActivityOption option : scenario.getActivityFacilities().getFacilities().get(facId).getActivityOptions().values()){
			if (option.getType().equals("home")){
				ActivityOptionImpl newOption = new ActivityOptionImpl("home");
				newOption.setOpeningTimes(option.getOpeningTimes());
				newOption.setCapacity(option.getCapacity());
				
				
				scenario.getActivityFacilities().getFacilities().get(newFacId).addActivityOption(newOption);
			}
		}
		
	}
	
	
	
	
	

}
