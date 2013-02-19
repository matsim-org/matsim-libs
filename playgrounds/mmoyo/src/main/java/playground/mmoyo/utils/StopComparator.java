package playground.mmoyo.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.utils.DataLoader;

/**
 * Compare counts transit stop coordinates versus schedule stops coordinates 
 */
public class StopComparator {
	TransitSchedule schedule;
	Counts counts;
	
	StopComparator(TransitSchedule schedule, Counts counts){
		this.schedule = schedule;
		this.counts = counts;
	}
	
	final String pref = ".1";
	final String tab = "\t";
	final String strCoor = "coord: ";
	final String noMatch = " not matching ";
	final String noStop = "stop from counts is not in transit schedule: ";
	private void run(){
		for(Count count : counts.getCounts().values()){
			Id pseudoStopId = new IdImpl(count.getLocId().toString() + pref);
			TransitStopFacility stop = schedule.getFacilities().get(pseudoStopId);
			
			if(stop !=null){
				if (!stop.getCoord().equals(count.getCoord())){
					System.out.println(tab + noMatch + stop.getId() + tab + stop.getCoord() + tab + count.getCoord() );
				}else{
					System.out.println(tab + strCoor + stop.getCoord().equals(count.getCoord()));	
				}
			}else{
				System.out.println(tab + noStop + count.getLocId());
			}
			
		}
	}
	
	public static void main(String[] args) {
		String countsFile = "../../";
		String schduleFile = "../../";
		
		DataLoader dataLoader = new DataLoader();
		TransitSchedule schedule = dataLoader.readTransitSchedule(schduleFile);
		Counts counts = dataLoader.readCounts(countsFile);
	
		StopComparator comparator = new StopComparator(schedule, counts);
		comparator.run();
	}

}
