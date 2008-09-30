package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Person;
import org.matsim.socialnetworks.algorithms.CompareTimeWindows;
import org.matsim.socialnetworks.mentalmap.TimeWindow;

public class TimeWindowCalcTimeCorrelations {

	public TimeWindowCalcTimeCorrelations(Hashtable<Facility,ArrayList<TimeWindow>> timeWindowMap){ 
		// First identify the overlapping Acts and the Persons involved
		Object[] facs = timeWindowMap.keySet().toArray();
		for(int i=0;i<facs.length;i++){
			Object[] visits= timeWindowMap.get(facs[i]).toArray();
			for(int ii=0;ii<visits.length;ii++){
				HashMap<String,ArrayList<Person>> othersMap = new HashMap<String,ArrayList<Person>>();
				TimeWindow tw1 = (TimeWindow) visits[ii];
				Person p1 = tw1.person;
				double arr_time=tw1.startTime;
				for (int iii=ii;iii<visits.length;iii++){
					TimeWindow tw2 = (TimeWindow) visits[iii];
					Person p2 = tw2.person;
					String actType2=tw2.act.getType();
					if(CompareTimeWindows.overlapTimePlaceType(tw1, tw2)){
						//note that p2 could be present twice;
						//if we are counting duration
						//of time overlap, we need to account for that
						if(othersMap.containsKey(actType2)){
							ArrayList<Person> list=othersMap.get(actType2);
							list.add(p2);
							othersMap.remove(actType2);
							othersMap.put(actType2,list);
						}else{
							ArrayList<Person> list=new ArrayList<Person>();
							list.add(p2);
							othersMap.put(actType2, list);
						}
					}
					
					//Stats here
					arr_time=arr_time+tw2.startTime;
				}
				arr_time=arr_time/(visits.length-ii);
				System.out.println(tw1.act.getType()+" "+arr_time);
////				Enumerate the keys of others
//				Object[] actTypes=othersMap.keySet().toArray();
//				for (int j=0;j<actTypes.length;j++){
//					ArrayList<Person> others = othersMap.get(actTypes[j]);
//					if(others.size()>0){
//						
////Compare the times here and record them for statistics
////This is a loop of activity, time, and person
//						System.out.println();
//					}
//				}
			}
		}
	}
}
