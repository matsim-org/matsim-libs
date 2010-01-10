package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationImpl;

import playground.jhackney.socialnetworks.socialnet.EgoNet;

public class PersonGetEgoNetGetPlans {

public PersonGetEgoNetGetPlans(){
	super();
}

	public PopulationImpl extract(final Person ego) throws Exception{

		PopulationImpl socialPlans= new ScenarioImpl().getPopulation();

		socialPlans.addPerson(ego);
		ArrayList<Person> alters = ((EgoNet)ego.getCustomAttributes().get(EgoNet.NAME)).getAlters();
		Iterator<Person> a_it=alters.iterator();
		while(a_it.hasNext()){
			socialPlans.addPerson(a_it.next());
		}
		return socialPlans;
	}
}
