package playground.tobiqui.master;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;


/**
 * 
 */

/**
 * @author tquick
 *
 */

public class TqMatsimPlansReader{

		protected Map<Id<Person>, ? extends Person> persons = new HashMap<Id<Person>, Person>(); 
		protected Map<Id<Person>, Person> personsSorted = new LinkedHashMap<Id<Person>, Person>(); 
		protected Map<Id<Person>, Integer> firstDepartures = new HashMap<Id<Person>, Integer>();
		protected Map<Id<Person>, Integer> firstDeparturesSorted = new LinkedHashMap<Id<Person>, Integer>();
		protected Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		protected Scenario scenarioSorted = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		protected Population pop = scenario.getPopulation();
		protected Population popSorted = scenarioSorted.getPopulation();
		
		public TqMatsimPlansReader() {
		}

		public Map<Id<Person>, Person> getPlans(String fileName) {
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimPopulationReader(scenario).readFile(fileName);
			persons = scenario.getPopulation().getPersons();
			
			return (Map<Id<Person>, Person>) persons;
		}
		
		public Map<Id<Person>, Person> sortPlans(Map<Id<Person>, Person> map){
			Iterator<?> i = map.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry pairs = (Map.Entry) i.next();
				Person p = (Person) pairs.getValue();
				PlanImpl pi = (PlanImpl) p.getSelectedPlan();
				ActivityImpl act = (ActivityImpl) pi.getFirstActivity();
				firstDepartures.put(p.getId(), (int) act.getEndTime());
			}
			firstDeparturesSorted = sortHashMapByValues(firstDepartures);
			
			Iterator<?> j = firstDeparturesSorted.keySet().iterator();
			while (j.hasNext()) {
				Id<Person> id = (Id<Person>) j.next();
				Person p = map.get(id);
				System.out.println("id: " + id + "     Person: " + p);
				personsSorted.put(id, p);
				popSorted.addPerson(p);
			}
			
			SortedPopulationWriter sortedPopulationWriter = new SortedPopulationWriter(popSorted);
			String filename = "D:/MA/workspace.bak/master/output/siouxfalls-2014/TestSorted.xml";

	  		sortedPopulationWriter.write(filename);
			
			return personsSorted;
		}
		
		public Population getSortedPopulation(){
			return this.popSorted;
		}
		
		public void writeSortedPopulation(Population population, String filename){
			SortedPopulationWriter sortedPopulationWriter = new SortedPopulationWriter(population);
	  		sortedPopulationWriter.write(filename);
		}
		
		public Map<Id<Person>,Integer> sortHashMapByValues(Map<Id<Person>, Integer> map){
			List<Id<Person>> mapKeys = new ArrayList(map.keySet());
			List<Integer> mapValues = new ArrayList(map.values());
			Collections.sort(mapKeys);
			Collections.sort(mapValues);
						
			Map<Id<Person>,Integer> sortedMap = new LinkedHashMap<Id<Person>,Integer>();
			
			Iterator valueIt = mapValues.iterator();
			while (valueIt.hasNext()){
				Object value = valueIt.next();
				Iterator keyIt = mapKeys.iterator();
				
				while(keyIt.hasNext()){
					Object key = keyIt.next();
					String comp1 = map.get(key).toString();
					String comp2 = value.toString();
					
					if (comp1.equals(comp2)){
						map.remove(key);
						mapKeys.remove(key);
						Id<Person> id = null;
						id = id.createPersonId(key.toString());
						System.out.println("id: " + id);
						sortedMap.put(id, (Integer) value);
						break;
					}
				}
			}
			return sortedMap;
		}

//		@SuppressWarnings("rawtypes")
//		public void test(Map<Id, Person> map){
//			Iterator<?> it = map.entrySet().iterator();
//			while (it.hasNext()) {
//				Map.Entry pairs = (Map.Entry) it.next();
//				Person p = (Person) pairs.getValue();
//				PlanImpl pi = (PlanImpl) p.getSelectedPlan();
//				ActivityImpl act = (ActivityImpl) pi.getFirstActivity();
//				LegImpl li = (LegImpl) pi.getNextLeg(act);
//				Route r = li.getRoute(); 
//				if (r.toString().contains("GenericRouteImpl")){
//					System.out.println(li.getMode() + " GRI " + act.getEndTime() + " -> " + pi.getNextLeg(pi.getNextActivity(li)).getMode() + " " + ((GenericRouteImpl) pi.getNextLeg(pi.getNextActivity(li)).getRoute()).getRouteDescription());
//				}else 
//					if (r.toString().contains("LinkNetworkRouteImpl")){
//						String route = ((LinkNetworkRouteImpl) li.getRoute()).getLinkIds().toString();
//						System.out.println(route.substring(1, route.length()-1).replace(",", "") + " " + act.getEndTime());
//					}else
//						System.out.println(r.toString() + " " + act.getEndTime());
//			}
//		}
	}


