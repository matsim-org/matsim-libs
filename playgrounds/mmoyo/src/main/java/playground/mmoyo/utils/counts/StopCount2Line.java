package playground.mmoyo.utils.counts;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.mmoyo.utils.DataLoader;

/**
 * get the transit lines that use the stops contained in a counts file 
 */
public class StopCount2Line {

	private void run (Counts counts, TransitSchedule schedule){
		Set<Id> lineIdsSet = new HashSet<Id>();
		
		for (TransitLine line : schedule.getTransitLines().values()){
			Set<Id> lineStopsIdsSet = new HashSet<Id>();
			for (TransitRoute route: line.getRoutes().values()){
				for (TransitRouteStop stop : route.getStops()){
					Id pseudoId = new IdImpl(Real2PseudoId.convertRealIdtoPseudo(stop.getStopFacility().getId().toString()));					
					//Id pseudoId = convertRealIdtoPseudo(stop.getStopFacility().getId());
					lineStopsIdsSet.add(pseudoId);
				}
			}
			
			for (Id  countId : counts.getCounts().keySet()){
				if (lineStopsIdsSet.contains(countId)){
					lineIdsSet.add(line.getId());
				}
			}
	   }
		
		final String comma = ",";
		for(Id id : lineIdsSet){
			System.out.print(id + comma) ;
		}
	}
	
	public static void main(String[] args) {
		String countsFile = "../../";
		String scheduleFile = "../../";
		
		DataLoader dataLoader = new DataLoader();
		Counts counts = dataLoader.readCounts(countsFile);
		TransitSchedule schedule= dataLoader.readTransitSchedule(scheduleFile);

		new StopCount2Line().run(counts, schedule);
	}
	
}