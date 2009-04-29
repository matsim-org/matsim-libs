package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.population.PopulationImpl;

public class PersonGetEgoNetGetPlans {

public PersonGetEgoNetGetPlans(){
	super();
}

	public PopulationImpl extract(final Person ego, final Population plans) throws Exception{


		PopulationImpl socialPlans=new PopulationImpl();

		socialPlans.addPerson(ego);
		ArrayList<Person> alters = ego.getKnowledge().getEgoNet().getAlters();
		Iterator<Person> a_it=alters.iterator();
		while(a_it.hasNext()){
			socialPlans.addPerson(a_it.next());
		}
		return socialPlans;
	}
}
