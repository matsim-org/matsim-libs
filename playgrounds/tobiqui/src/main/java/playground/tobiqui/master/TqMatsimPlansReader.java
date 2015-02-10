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
import java.util.TreeSet;

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
import org.matsim.population.algorithms.PlansFilterByLegMode;
import org.matsim.population.algorithms.PlansFilterByLegMode.FilterType;


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
		protected Scenario scenario;
		protected Scenario scenarioSorted;
		protected Population pop;
		protected Population popSorted;
		
		public TqMatsimPlansReader(String fileName) {
			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.pop = this.scenario.getPopulation();
			new MatsimPopulationReader(this.scenario).readFile(fileName);
			
			this.scenarioSorted = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.popSorted = this.scenarioSorted.getPopulation();
		}

		public List<Person> getPlans() {
			this.persons = this.scenario.getPopulation().getPersons();
			
			return new ArrayList<Person>(this.persons.values());
		}
		
		public List<Person> getPlansWithModeCareOnly() {
			this.persons = this.pop.getPersons();
			TreeSet<Id<Person>> pid_set = new TreeSet<>();	// ids of persons to remove
			for (Person person : this.persons.values()){

				for (int j=1; j<person.getSelectedPlan().getPlanElements().size(); j+=2) {
					LegImpl leg = (LegImpl)person.getSelectedPlan().getPlanElements().get(j);
					if (!(leg.getMode().equals("car"))) {
						pid_set.add(person.getId());
					}
				}
			}
			// okay, now remove in a 2nd step all persons we do no longer need
			Iterator<Id<Person>> pid_it = pid_set.iterator();
			while (pid_it.hasNext()) {
				Id<Person> pid = pid_it.next();
				this.persons.remove(pid);
			}
			
//			PlansFilterByLegMode filter = new PlansFilterByLegMode("car", FilterType.keepPlansWithOnlyThisMode);
//			
//			filter.run(this.pop);filter.run(this.pop);filter.run(this.pop);filter.run(this.pop);filter.run(this.pop);filter.run(this.pop);filter.run(this.pop);filter.run(this.pop);
//			
//			this.persons = this.pop.getPersons();
			
			return new ArrayList<Person>(this.persons.values());
		}
		
		public Map<Id<Person>, Person> sortPlans(Map<Id<Person>, Person> map){
			Iterator<?> i = map.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry pairs = (Map.Entry) i.next();
				Person p = (Person) pairs.getValue();
				PlanImpl pi = (PlanImpl) p.getSelectedPlan();
				ActivityImpl act = (ActivityImpl) pi.getFirstActivity();
				this.firstDepartures.put(p.getId(), (int) act.getEndTime());
			}
			this.firstDeparturesSorted = sortHashMapByValues(this.firstDepartures);
			
			Iterator<?> j = this.firstDeparturesSorted.keySet().iterator();
			while (j.hasNext()) {
				Id<Person> id = (Id<Person>) j.next();
				Person p = map.get(id);
				this.personsSorted.put(id, p);
				this.popSorted.addPerson(p);
			}
			
			return this.personsSorted;
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
						sortedMap.put(id, (Integer) value);
						break;
					}
				}
			}
			return sortedMap;
		}
	}


