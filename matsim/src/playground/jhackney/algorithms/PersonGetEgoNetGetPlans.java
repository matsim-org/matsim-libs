package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

import playground.jhackney.socialnetworks.socialnet.EgoNet;

public class PersonGetEgoNetGetPlans {

public PersonGetEgoNetGetPlans(){
	super();
}

	public PopulationImpl extract(final PersonImpl ego, final Population plans) throws Exception{


		PopulationImpl socialPlans=new PopulationImpl();

		socialPlans.addPerson(ego);
		ArrayList<PersonImpl> alters = ((EgoNet)ego.getCustomAttributes().get(EgoNet.NAME)).getAlters();
		Iterator<PersonImpl> a_it=alters.iterator();
		while(a_it.hasNext()){
			socialPlans.addPerson(a_it.next());
		}
		return socialPlans;
	}
}
