package playground.mmoyo.utils.counts;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;

/**
 * get the transit lines that use the stops contained in a counts file 
 */
public class StopCount2Line {

	private void run (Counts counts, TransitSchedule schedule){
		Set<Id<TransitLine>> lineIdsSet = new HashSet<>();
		
		for (TransitLine line : schedule.getTransitLines().values()){
			Set<Id<Link>> lineStopsIdsSet = new HashSet<>();
			for (TransitRoute route: line.getRoutes().values()){
				for (TransitRouteStop stop : route.getStops()){
					Id<Link> pseudoId = Id.create(Real2PseudoId.convertRealIdtoPseudo(stop.getStopFacility().getId().toString()), Link.class);					
					//Id pseudoId = convertRealIdtoPseudo(stop.getStopFacility().getId());
					lineStopsIdsSet.add(pseudoId);
				}
			}
			
			for (Id<Link> countId : counts.getCounts().keySet()){
				if (lineStopsIdsSet.contains(countId)){
					lineIdsSet.add(line.getId());
				}
			}
	   }
		
		final String comma = ",";
		for(Id<TransitLine> id : lineIdsSet){
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