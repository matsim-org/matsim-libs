package playground.droeder.bvg09;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;

public class DaHafas2VisumMapper5 extends AbstractDaVisum2HafasMapper {

	public static void main(String[] args) {
		DaHafas2VisumMapper5 mapper = new DaHafas2VisumMapper5(150);
		mapper.run();
	}

	public DaHafas2VisumMapper5(double dist2Match) {
		super(dist2Match);
	}

	@Override
	protected Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute) {
		SortedMap<Integer, Integer> sortedMatch = new TreeMap<Integer, Integer>();

		
		
		//no matching possible if visRoute longer then hafRoute
		if(visRoute.getStops().size() > hafRoute.getStops().size()) return null;
		
		/*
		 * if hRoute is at the end and vRoute not return null
		 * else if all stops prematched and in the correct order, return matched
		 * 
		 * h xxxxx	xxxxx	xxxxx	xxxxx
		 * v xxxxx	 xxxx	 xxx	x xxx
		 */
		sortedMatch = this.matchPrematchedStraightOrderRoute(visRoute, hafRoute);
		if(sortedMatch == null){
			return null;
		}else if(sortedMatch.size() == visRoute.getStops().size()){
			return position2IdPair(sortedMatch, visRoute, hafRoute);
		}
		
		Map<Integer, Integer> temp = matchNotPrematched(visRoute, hafRoute, sortedMatch);
		
		if (temp == null){
			return null;
		}else{
			sortedMatch.putAll(temp);
		}
		
		if(sortedMatch.size() == visRoute.getStops().size()){
			return position2IdPair(sortedMatch, visRoute, hafRoute);
		}else{
			return null;
		}
	}
	
	private SortedMap<Integer, Integer> matchNotPrematched(TransitRoute visRoute, TransitRoute hafRoute, SortedMap<Integer, Integer> preMatched){
		int hPos = 0, vPos = 0;
		
		for(Entry<Integer, Integer> e : preMatched.entrySet()){
			
		}
 	
		return null;
	}
	
	
	private SortedMap<Integer, Integer> matchPrematchedStraightOrderRoute(TransitRoute visRoute, TransitRoute hafRoute) {
		SortedMap<Integer, Integer> sortedMatch = new TreeMap<Integer, Integer>();
		ListIterator<TransitRouteStop> vIt = visRoute.getStops().listIterator();
		ListIterator<TransitRouteStop> hIt;
		Id vStop;
		Id hStop;
		
		while(vIt.hasNext()){
			vStop = vIt.next().getStopFacility().getId();
			hIt = hafRoute.getStops().listIterator();
			if(!hIt.hasNext()) return null;
			while(hIt.hasNext()){
				hStop = hIt.next().getStopFacility().getId();
				if(this.preVisum2HafasMap.containsKey(vStop) && this.preVisum2HafasMap.get(vStop).equals(hStop)){
					sortedMatch.put(vIt.previousIndex(), hIt.previousIndex());
				}
			}
		}
		return sortedMatch;
	}

	private Map<Id, Id> position2IdPair(Map<Integer, Integer> sorted, TransitRoute vRoute, TransitRoute hRoute){
		Map<Id, Id> matched = new HashMap<Id, Id>();
		
		for(Entry<Integer, Integer> e : sorted.entrySet()){
			matched.put(vRoute.getStops().get(e.getKey()).getStopFacility().getId(), hRoute.getStops().get(e.getValue()).getStopFacility().getId());
		}
		
		return matched;
	}
}
