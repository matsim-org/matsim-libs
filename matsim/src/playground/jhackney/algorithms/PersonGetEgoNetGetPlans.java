package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.socialnetworks.socialnet.SocialNetwork;

public class PersonGetEgoNetGetPlans {

public PersonGetEgoNetGetPlans(){
	super();
}
	
	public Population extract(Person ego, Population plans) throws Exception{
		
	
		Population socialPlans=new Population();
		
		socialPlans.addPerson(ego);
		ArrayList<Person> alters = ego.getKnowledge().getEgoNet().getAlters();
		Iterator<Person> a_it=alters.iterator();
		while(a_it.hasNext()){
			socialPlans.addPerson((Person) a_it.next());
		}
		return socialPlans;
	}
}
