package playground.dhosse.gap.scenario.population;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.matrices.Matrix;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.population.personGroups.CreateDemandV2;
import playground.dhosse.scenarios.generic.population.io.commuters.CommuterDataElement;
import playground.dhosse.scenarios.generic.population.io.commuters.CommuterFileReader;
import playground.dhosse.scenarios.generic.population.io.mid.MiDCsvReader;
import playground.dhosse.scenarios.generic.population.io.mid.MiDPersonGroupTemplates;

public class PlansCreatorV2 {
	
	private static final Logger log = Logger.getLogger(PlansCreatorV2.class);
	
	public static Population createPlans(Scenario scenario, String commuterFilename, String reverseCommuterFilename, Map<String,Matrix> matrices){
		
		//create a commuter reader and add municipalities to the filter in order to create commuter relations
		//that start or end in these municipalities
		CommuterFileReader cdr = new CommuterFileReader();
		
//		cdr.addFilter("09180"); //GaPa (Kreis)
		cdr.addFilter("09180113"); //Bad Bayersoien
		cdr.addFilter("09180112"); //Bad Kohlgrub
		cdr.addFilter("09180114"); //Eschenlohe
		cdr.addFilter("09180115"); //Ettal
		cdr.addFilter("09180116"); //Farchant
		cdr.addFilter("09180117"); //Garmisch-Partenkirchen
		cdr.addFilter("09180118"); //Grainau
		cdr.addFilter("09180119"); //Großweil
		cdr.addFilter("09180122"); //Krün
		cdr.addFilter("09180123"); //Mittenwald
		cdr.addFilter("09180124"); //Murnau a Staffelsee
		cdr.addFilter("09180125"); //Oberammergau
		cdr.addFilter("09180126"); //Oberau
		cdr.addFilter("09180127"); //Ohlstadt
		cdr.addFilter("09180128"); //Riegsee
		cdr.addFilter("09180129"); //Saulgrub
		cdr.addFilter("09180131"); //Schwaigen
		cdr.addFilter("09180132"); //Seehausen a Staffelsee
		cdr.addFilter("09180134"); //Uffind a Staffelsee
		cdr.addFilter("09180135"); //Unterammergau
		cdr.addFilter("09180136"); //Wallgau
//		cdr.setSpatialFilder("091");
		
		cdr.read(reverseCommuterFilename, true);
		cdr.read(commuterFilename, false);
		
		//the actual demand generation
		createPersonsWithDemographicData(scenario, cdr.getCommuterRelations(), matrices);
		
		//remove persons with plans containing less than three plan elements
		Set<Person> toBeRemoved = new HashSet<>();
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			if(person.getSelectedPlan().getPlanElements().size() < 3){
				
				toBeRemoved.add(person);
				
			}
			
		}
		
		for(Person p : toBeRemoved){
			scenario.getPopulation().getPersons().remove(p.getId());
		}
		
		log.info(toBeRemoved.size() + " persons had plans containing less than three plan elements and had thus to be removed from the population...");
		
		return scenario.getPopulation();
		
	}
	
	private static void createPersonsWithDemographicData(Scenario scenario, Map<String, CommuterDataElement> relations, Map<String,Matrix> matrices){
		
		MiDPersonGroupTemplates templates = new MiDPersonGroupTemplates();
		
		MiDCsvReader reader = new MiDCsvReader();
		reader.readV2("/home/dhosse/bayern_laendl_Wege.csv", templates);
		templates.setWeights();

		CreateDemandV2.getNinetyPctDistances().put(TransportMode.car, new Double(14560));
		CreateDemandV2.getNinetyPctDistances().put(TransportMode.ride, new Double(19091));
		CreateDemandV2.getNinetyPctDistances().put(TransportMode.bike, new Double(8719));
		CreateDemandV2.getNinetyPctDistances().put(TransportMode.walk, new Double(2939));
		CreateDemandV2.getNinetyPctDistances().put(TransportMode.pt, new Double(21515));
		
		for(Entry<String, Municipality> entry : Municipalities.getMunicipalities().entrySet()){
			
//			CreateDemandV2.runTryout(entry.getKey(), 6, 17, entry.getValue().getnStudents(), scenario, templates, matrices);
			
			int nCommuters = 0;
			List<String> keysToRemove = new ArrayList<>();
			
			for(String relation : relations.keySet()){
				
				String[] relationParts = relation.split("_");
				
				if(relationParts[0].startsWith(entry.getKey())){
	
					nCommuters += relations.get(relation).getCommuters();
					
					if(relationParts[1].startsWith("09180")){
						
//						CreateDemandV3.createCommuters(scenario, relationParts[0], relationParts[1], relations.get(relation).getCommuters());
//						CreateDemandV2.createCommuters(relationParts[0], relationParts[1], 18, 65, relations.get(relation), scenario, templates, matrices);
//						keysToRemove.add(relation);
						
					}
					
				}
				
			}
			
			for(String s : keysToRemove){
				
				relations.remove(s);
				
			}
			
//			CreateDemandV2.runTryout(entry.getKey(), 18, 65, entry.getValue().getnAdults() - nCommuters, scenario, templates, matrices);
//			CreateDemandV2.runTryout(entry.getKey(), 66, 100, entry.getValue().getnPensioners(), scenario, templates, matrices);
			
		}
		
//		CreateCommutersFromElsewhere.run(scenario, relations.values());
		
	}
	
	public static double createRandomTimeShift(double variance){
		
		//draw two random numbers [0;1] from uniform distribution
		double r1 = Global.random.nextDouble();
		double r2 = Global.random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = variance * 3600 * normal;
		
		return endTime;
		
	}
	
}