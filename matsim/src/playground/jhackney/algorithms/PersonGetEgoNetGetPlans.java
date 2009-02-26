package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.PopulationImpl;

public class PersonGetEgoNetGetPlans {

public PersonGetEgoNetGetPlans(){
	super();
}

	public Population extract(final Person ego, final Population plans) throws Exception{


		Population socialPlans=new PopulationImpl();

		socialPlans.addPerson(ego);
		ArrayList<Person> alters = ego.getKnowledge().getEgoNet().getAlters();
		Iterator<Person> a_it=alters.iterator();
		while(a_it.hasNext()){
			socialPlans.addPerson(a_it.next());
		}
		return socialPlans;
	}
}
