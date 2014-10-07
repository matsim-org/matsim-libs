package playground.mmoyo.utils.counts;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.utils.DataLoader;

/**remove counts that are not present in a given transit schedule
 * (designed for 24 hours counts)
 * */
public class CountsStop_VS_ScheduleStop {
	
	void run (final TransitSchedule schedule, Counts counts){
		Set<Id<Link>> pseudoScheduleStopIds = new HashSet<>();
	
		//create set of transit schedule "pseudo stops"
		//final char point = '.';
		for (TransitStopFacility stop : schedule.getFacilities().values()){
			//String str_pseudoId = stop.getId().toString();
			//int pointPos = str_pseudoId.indexOf(point);
			//str_pseudoId = str_pseudoId.substring(0, pointPos);
			String str_pseudoId = Real2PseudoId.convertRealIdtoPseudo(stop.getId().toString());
			Id<Link> pseudoId = Id.create(str_pseudoId, Link.class);
			pseudoScheduleStopIds.add(pseudoId);
		}
	
		//find counts ids that are not present in schedule
		Set<Id<Link>> removableIds = new HashSet<>();
		for(Id<Link> countId : counts.getCounts().keySet()){
			if (!pseudoScheduleStopIds.contains(countId)){
				removableIds.add(countId);
			}
		}
	
		//remove counts of stops that are not present in transit schedule
		final String removing = "removing: ";
		System.out.println(removableIds.size());
		for(Id<Link> removableId : removableIds){
			System.out.println(removing + removableId);
			counts.getCounts().remove(removableId);
		}
	}
	
	/*
	void run (final TransitSchedule schedule, Counts counts){
		Set<Id> goodIds = new HashSet<Id>();
		
		int i=0;
		final char point = '.';
		for (TransitStopFacility stop : schedule.getFacilities().values()){
			String str_pseudoId = stop.getId().toString();
			int pointPos = str_pseudoId.indexOf(point);
			str_pseudoId = str_pseudoId.substring(0, pointPos);
			//System.out.println(stop.getId() +  " " + str_pseudoId);
		
			Id pseudoId = Id.create(str_pseudoId);
			Count count = counts.getCount(pseudoId);
			if (count==null){
				System.out.println(pseudoId);
				goodIds.add(pseudoId);
			}
		}
		
		//remove counts of stops that are not present in transit schedule
		for (Id id : goodIds){
			counts.getCounts().remove(id);
		}
	}
	*/
		
	public static void main(String[] args) {
		String scheduleFile = "../../input/newDemand/transitSchedule.xml"; // "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		String countsFile = "../../input/newDemand/bvg.run189.10pct.100.ptLineCounts.txtaggregatedCountsFilteredNewSchedule.xmlWOzeroValues.xml"; // "../../berlin-bvg09/ptManuel/berlin09/Filtered24hrs_counts.xml"; 
		
		DataLoader loader = new DataLoader();
		TransitSchedule schedule = loader.readTransitSchedule(scheduleFile);
		Counts counts = loader.readCounts(countsFile);
		new CountsStop_VS_ScheduleStop().run(schedule, counts);
	
		File file =new File(countsFile);
		//new CountsWriter(counts).write(file.getParentFile().getPath()+ "/Filtered4Schedule" + file.getName());
	}

}