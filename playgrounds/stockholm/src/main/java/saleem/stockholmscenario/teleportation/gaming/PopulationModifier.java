package saleem.stockholmscenario.teleportation.gaming;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationModifier {
	public static void main(String[] args){
		String path = "./ihop2/matsim-input/config.xml";
	    Config config = ConfigUtils.loadConfig(path);
	    final Scenario scenario = ScenarioUtils.loadScenario(config);
	    int numberppl = 1500; //Additional person to add, for Alvsjo 5000
	    Coord origincords = new Coord(672930, 6576324);
	    PopulationModifier popmod = new PopulationModifier();
	    
//	    ArrayList<Person> sortedpersons = popmod.sortPersonsPerDistance(scenario, origincords, numberppl);
//	    ArrayList<Person> sortedpersons = popmod.sortEmployedPersonsPerDistance(scenario, origincords, numberppl);
	    ArrayList<Person> sortedpersons = popmod.sortUnEmployedPersonsPerDistance(scenario, origincords, numberppl);
	    
//	    popmod.duplicatePersons(scenario.getPopulation(), sortedpersons.subList(0, numberppl));
//	    popmod.printSortedPersonsCoordinates(origincords, "./ihop2/matsim-input/10mFarstaCentrum.xy", sortedpersons, numberppl);
//	    popmod.writePopulation(scenario, "./ihop2/demand-output/10mFarstaCentrum.xml");
	
	    //For Arsta
	    ArrayList<Person> addedpersons = popmod.duplicatePersonsArsta(scenario.getNetwork(), scenario.getPopulation(), sortedpersons.subList(0, numberppl));
	    popmod.printSortedPersonsCoordinates(origincords, "./ihop2/matsim-input/10uArstafaltet.xy", addedpersons, numberppl);
	    popmod.writePopulation(scenario, "./ihop2/demand-output/10uArstafaltet.xml");
//	    
//	    String pathnew = "./ihop2/matsim-input/confignew.xml";
//	    Config confignew = ConfigUtils.loadConfig(pathnew);
//	    final Scenario scenarionew = ScenarioUtils.loadScenario(confignew);
//	    popmod.duplicatePersons(scenarionew.getPopulation(), sortedpersons.subList(0, numberppl));
//	    popmod.printSortedPersonsCoordinates(origincords, "./ihop2/matsim-input/alvsjounemployed1000.xy", sortedpersons, numberppl);
//	    popmod.writePopulation(scenarionew, "./ihop2/demand-output/alvsjounemployed.xml");
	    
    
	}
	public Boolean employed(Person person){
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (PlanElement planelement : planElements) {
    		if (planelement instanceof Activity) {
    			Activity activity = (Activity) planelement;
    			if(activity.getType().toString().contains("w")){
    				return true;
    			}
    		}
		}
		return false;
	}
	public ArrayList<Person> sortEmployedPersonsPerDistance(Scenario scenario, Coord cords, int numofppl){
		Population population = scenario.getPopulation();
	    Map<Id<Person>, ? extends Person> persons = population.getPersons();
	    ArrayList<Person> sortedpersons = new ArrayList<Person>();
	    ArrayList<Double> distances = new ArrayList<Double>();
		for( Person person: persons.values()){
			if(employed(person)){
				Coord homecords = PopulationUtils.getFirstActivity(person.getSelectedPlan()).getCoord();//Home coordinates
				double distance = NetworkUtils.getEuclideanDistance(cords, homecords);
				int i=0;
				for(;i<distances.size();i++){
					if(distances.get(i)<distance){
						;
					}else{
						break;
					}
				}
				distances.add(i, distance);
				sortedpersons.add(i, person);
			}
			
		}
		return  sortedpersons;
	}
	public ArrayList<Person> sortUnEmployedPersonsPerDistance(Scenario scenario, Coord cords, int numofppl){
		Population population = scenario.getPopulation();
	    Map<Id<Person>, ? extends Person> persons = population.getPersons();
	    ArrayList<Person> sortedpersons = new ArrayList<Person>();
	    ArrayList<Double> distances = new ArrayList<Double>();
		for( Person person: persons.values()){
			if(!employed(person)){
				Coord homecords = PopulationUtils.getFirstActivity(person.getSelectedPlan()).getCoord();//Home coordinates
				double distance = NetworkUtils.getEuclideanDistance(cords, homecords);
				int i=0;
				for(;i<distances.size();i++){
					if(distances.get(i)<distance){
						;
					}else{
						break;
					}
				}
				distances.add(i, distance);
				sortedpersons.add(i, person);
			}
			
		}
		return  sortedpersons;
	}
	
	public void removeAllPersons( Population population, ArrayList<Person> sortedpersons){
		Iterator<Person> sortedpersonsiter = sortedpersons.iterator();
		
		while(sortedpersonsiter.hasNext()){
			Person person = sortedpersonsiter.next();
//			population.getPersons().remove(person.getId(), person);
			population.getPersons().remove(person.getId() );
			// I changed this to make the code compile.  kai, 9/sep/16
		}
	}
	public ArrayList<Person> sortPersonsPerDistance(Scenario scenario, Coord cords, int numofppl){
		Population population = scenario.getPopulation();
	    Map<Id<Person>, ? extends Person> persons = population.getPersons();
	    ArrayList<Person> sortedpersons = new ArrayList<Person>();
	    ArrayList<Double> distances = new ArrayList<Double>();
		for( Person person: persons.values()){
			Coord homecords = PopulationUtils.getFirstActivity(person.getSelectedPlan()).getCoord();//Home coordinates
			double distance = NetworkUtils.getEuclideanDistance(cords, homecords);
			int i=0;
			for(;i<distances.size();i++){
				if(distances.get(i)<distance){
					;
				}else{
					break;
				}
			}
			distances.add(i, distance);
			sortedpersons.add(i, person);
		}
		return  sortedpersons;
	}
	public void printSortedPersons(Coord cords, ArrayList<Person> sortedpersons){
		Iterator<Person> sortedpersonsiter = sortedpersons.iterator();
		while(sortedpersonsiter.hasNext()){
			Person person = sortedpersonsiter.next();
			Coord homecords = PopulationUtils.getFirstActivity(person.getSelectedPlan()).getCoord();//Home coordinates
			double distance = NetworkUtils.getEuclideanDistance(cords, homecords);
			System.out.println("Person: " + person.getId()  + " Coord: " + homecords.getX() + " ; " + homecords.getY() + " Distance: " + distance);
		}
	}
	public void printSortedPersonsCoordinates(Coord cords, String filepath, ArrayList<Person> sortedpersons, int number){
		Iterator<Person> sortedpersonsiter = sortedpersons.iterator();
		String str = "";int count = 0;
		while(sortedpersonsiter.hasNext() && count++ < number){
			Person person = sortedpersonsiter.next();
			Coord homecords = PopulationUtils.getFirstActivity(person.getSelectedPlan()).getCoord();//Home coordinates
			str = str + homecords.getX() + "\t" + homecords.getY()+"\n";
		}	try { 
				File file=new File(filepath);
				FileOutputStream fileOutputStream=new FileOutputStream(file);
				fileOutputStream.write(str.getBytes());
				fileOutputStream.close();
	       
	    	} catch(Exception ex) {
	    		//catch logic here
	    	}
	}
	public void duplicatePersons(Population population, List<Person> sortedpersons){
		for (Person person: sortedpersons){
			Person newPerson = population.getFactory().createPerson(Id.createPersonId(person.getId().toString()+"a"));
			final List<? extends Plan> fromPlanList = person.getPlans();
			for (Plan fromPlan : fromPlanList) {
				final Plan toPlan = PopulationUtils.createPlan(newPerson);
				PopulationUtils.copyFromTo(fromPlan, toPlan);
				newPerson.addPlan(toPlan);
			}
			population.addPerson(newPerson);
		}
	}
	public ArrayList<Person>  duplicatePersonsArsta(Network network, Population population, List<Person> sortedpersons){
		ArrayList<Person> addedpersons = new ArrayList<Person>();
		for (Person person: sortedpersons){
			Person newPerson = population.getFactory().createPerson(Id.createPersonId(person.getId().toString()+"a"));
			final List<? extends Plan> fromPlanList = person.getPlans();
			for (Plan fromPlan : fromPlanList) {
				final Plan toPlan = PopulationUtils.createPlan(newPerson);
				PopulationUtils.copyFromTo(fromPlan, toPlan);
				double xrad = (Math.random()-0.5)*1000;
				double yrad = (Math.random()-0.5)*1000;
				Coord coord = new Coord(672930 + xrad, 6576324 + yrad);
				PopulationUtils.getFirstActivity(toPlan).setCoord(coord);
				PopulationUtils.getLastActivity(toPlan).setCoord(coord);
				Link link = NetworkUtils.getNearestLink(network, coord);
				PopulationUtils.getFirstActivity(toPlan).setLinkId(link.getId());
				PopulationUtils.getLastActivity(toPlan).setLinkId(link.getId());
				newPerson.addPlan(toPlan);
				
			}
			population.addPerson(newPerson);
			addedpersons.add(newPerson);
		}
		return addedpersons;
	}
	public void writePopulation(Scenario scenario, String filename){
		PopulationWriter pwriter = new PopulationWriter(scenario.getPopulation());
		pwriter.writeV5(filename);
	}
}
