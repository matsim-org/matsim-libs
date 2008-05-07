package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.gbl.Gbl;

public class PersonsRandomizeId {

	public PersonsRandomizeId(Plans plans){
			
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
			p.setId(newIds.get(j).toString());
			j++;
		}
	}

	private ArrayList<Integer> makeNewIds(int numPers2, int minId, int maxId) {
		// TODO Auto-generated method stub
		int offset=Gbl.random.nextInt(maxId);
		ArrayList<Integer> newIds=new ArrayList<Integer>();
		for (int i = minId + offset; i<maxId+offset; i++){
			newIds.add(i);
		}
		java.util.Collections.shuffle(newIds, Gbl.random);
		return newIds;
	}

}
