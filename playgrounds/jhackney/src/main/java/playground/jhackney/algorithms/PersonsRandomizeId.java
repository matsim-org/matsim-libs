package playground.jhackney.algorithms;

import java.util.ArrayList;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationImpl;

public class PersonsRandomizeId {

	public PersonsRandomizeId(PopulationImpl plans){
			
		int minId=14141414;
		int maxId=0;
		int numPers=0;
		
		plans.setName("created by \'"+this.getClass().getName()+"\'");
		
		numPers=plans.getPersons().values().size();
		maxId=minId+numPers;
		ArrayList<Integer> newIds=makeNewIds(numPers, minId, maxId);
		int j=0;
		for (Person p : plans.getPersons().values()) {
		
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
		int offset=MatsimRandom.getRandom().nextInt(maxId);
		ArrayList<Integer> newIds=new ArrayList<Integer>();
		for (int i = minId + offset; i<maxId+offset; i++){
			newIds.add(i);
		}
		java.util.Collections.shuffle(newIds, MatsimRandom.getRandom());
		return newIds;
	}

}
