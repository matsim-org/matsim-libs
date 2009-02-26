package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Population;

public class PersonsRandomizeId {

	public PersonsRandomizeId(Population plans){
			
		int minId=14141414;
		int maxId=0;
		int numPers=0;
		
		plans.setName("created by \'"+this.getClass().getName()+"\'");
		
		numPers=plans.getPersons().values().size();
		maxId=minId+numPers;
		ArrayList<Integer> newIds=makeNewIds(numPers, minId, maxId);
		Iterator<Person> pIt=plans.getPersons().values().iterator();
		
		int j=0;
		while(pIt.hasNext()){
			Person p=pIt.next();
//			int id=Integer.valueOf(p.getId().toString()).intValue();
//			if(minId>id){
//				minId=id;
//			}
//			if(maxId<id){
//				maxId=id;
//			}
			p.setId(new IdImpl(newIds.get(j)));
			j++;
		}
	}

	private ArrayList<Integer> makeNewIds(int numPers2, int minId, int maxId) {
		// TODO Auto-generated method stub
		int offset=MatsimRandom.random.nextInt(maxId);
		ArrayList<Integer> newIds=new ArrayList<Integer>();
		for (int i = minId + offset; i<maxId+offset; i++){
			newIds.add(i);
		}
		java.util.Collections.shuffle(newIds, MatsimRandom.random);
		return newIds;
	}

}
