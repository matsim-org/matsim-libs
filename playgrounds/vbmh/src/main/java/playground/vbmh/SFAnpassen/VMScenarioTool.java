package playground.vbmh.SFAnpassen;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesWriter;

import playground.vbmh.vmEV.EV;
import playground.vbmh.vmEV.EVList;
import playground.vbmh.vmEV.EVListWriter;

public class VMScenarioTool {

	static HashMap<Id<ActivityFacility>, LinkedList <Person>> homes = new HashMap<>();
	static Scenario scenario;
	static double xCoord = 678773;
	static double yCoord = 4908813;
	static Coord brookings = new Coord(xCoord, yCoord);

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		

		// E I N S T E L L U N G EN
		String scenarioName = "140325_1";
		
		System.out.println("scenario name?");
		Scanner s = new Scanner(System.in);
		scenarioName=s.next();
		
		int anzahl_agents = 84110;
		String networkFile = "input/SF_PLUS/network/siouxFalls_network_OSM_brookings.xml";
		String inputPricingFile = "input/SF_PLUS/pricing/parking_pricing_models_demo.xml";
		String outputFileF = "input/SF_PLUS/Scenario/"+scenarioName+"/facilities.xml";
		String outputFileP = "input/SF_PLUS/Scenario/"+scenarioName+"/population.xml";
		String evOutputFile = "input/SF_PLUS/Scenario/"+scenarioName+"/evs.xml";
		String newConfigFileName = "input/SF_PLUS/Scenario/"+scenarioName+"/config.xml";
		String parkingOutput = "input/SF_PLUS/Scenario/"+scenarioName+"/parking.xml";
		//--
		String inputFileConf = "input/Original/config_SF_default.xml";
		String inputFileLeereConf = "input/Schreiben/defaultconfig.xml";
		//--------------------------------
		
		
		// A G G L O 
		double xCoord = 678773;
		double yCoord = 4908813;
		Coord brookings = new Coord(xCoord, yCoord);
		//------------------
				
		//Verzeichnisse erstellen
		try{
		File dir = new File("input/SF_PLUS/Scenario/"+scenarioName);
			dir.mkdir();
		}catch(Exception e){
			System.out.println("Verzeichniss wurde nicht angelegt");
		}
		//-----
		
		
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(inputFileConf));
		
		//neue config anpassen
		Config newConfig = ConfigUtils.loadConfig(inputFileLeereConf);
		newConfig.getModule("controler").addParam("outputDirectory", "output/"+scenarioName);
		newConfig.getModule("network").addParam("inputNetworkFile", networkFile);
		newConfig.getModule("plans").addParam("inputPlansFile", outputFileP);
		newConfig.getModule("facilities").addParam("inputFacilitiesFile", outputFileF);
		newConfig.getModule("VM_park").addParam("inputParkingFile", parkingOutput);
		newConfig.getModule("VM_park").addParam("inputPricingFile", inputPricingFile);
		newConfig.getModule("VM_park").addParam("inputEVFile", evOutputFile);
		ConfigWriter configWriter = new ConfigWriter(newConfig);
		configWriter.write(newConfigFileName);
		//--
		
		
		
		//Reduzieren
		Random zufall = new Random();
		int i = 0;
		double wkeit = anzahl_agents/scenario.getPopulation().getPersons().size();
		Map<Id<Person>, ? extends Person> personMap = scenario.getPopulation().getPersons();
		LinkedList <Id<Person>> personsNotUsed = new LinkedList <Id<Person>>();
		for (Person p : scenario.getPopulation().getPersons().values()) {
			Person pa = p;
			System.out.println(PersonUtils.getCarAvail(pa));
			if(PersonUtils.getCarAvail(pa)!="never" && zufall.nextDouble()<wkeit && i<anzahl_agents){
				//population.addPerson(pa);
				
				System.out.println("Autofahrer hinzugefuegt");
				i+=1;
			}else{
				personsNotUsed.add(pa.getId());
			}
		}
		
		for (Id personId : personsNotUsed){
			personMap.remove(personId);
		}
		
		
		

		//zuegeln:
		
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
		
		
		
		// EVs
		EVListWriter evWriter = new EVListWriter();
		EVList evList = new EVList();
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
		evWriter.write(evList, evOutputFile);
		
		
		
		
		
		//Parkplaetze bauen:
		
		String [] parameter = {newConfigFileName, parkingOutput};
		CreateDemoParking.main(parameter);
		
		
		

	}
	
	static void move(Id facId){
		Id<ActivityFacility> newFacId = Id.create(facId.toString()+"_B", ActivityFacility.class);
		for (Person person : homes.get(facId)){
			PersonImpl personImpl = (PersonImpl) person;
			for(PlanElement planElement : personImpl.getSelectedPlan().getPlanElements()){
				if(planElement.getClass().equals(ActivityImpl.class)){
					System.out.println("Activity gefunden");
					ActivityImpl activity = (ActivityImpl) planElement;
					if (activity.getType().equals("home")){
						activity.getCoord().setXY(xCoord, yCoord);
						activity.setEndTime(activity.getEndTime()-2400); //!! 40 minuten frueher wegfahren - gute idee??
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
	
	
	static double probabilityOfEVOwnership(Person person){
		
		return 0.2; //!! BESSER MACHEN
	}
	
	
	
	

}
