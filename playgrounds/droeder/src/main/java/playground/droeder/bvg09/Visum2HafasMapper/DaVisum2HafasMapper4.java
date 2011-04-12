package playground.droeder.bvg09.Visum2HafasMapper;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class DaVisum2HafasMapper4 extends AbstractDaVisum2HafasMapper {

	public static void main(String[] args) {
		DaVisum2HafasMapper4 mapper = new DaVisum2HafasMapper4(150.0);
		mapper.run();
		
	}

	public DaVisum2HafasMapper4(double dist2Match) {
		super(dist2Match);
	}

	@Override
	protected Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute) {
		SortedMap<Integer, Triple> sortedMatch = new TreeMap<Integer, Triple>();

		
		
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
			return triple2IdPair(sortedMatch);
		}
		
		Map<Integer, Triple> temp = matchNotPrematched(visRoute, hafRoute, sortedMatch);
		
		if (temp == null){
			return null;
		}
		
		if(sortedMatch.size() == visRoute.getStops().size()){
			return triple2IdPair(sortedMatch);
		}else{
			return null;
		}
	}
	
	private SortedMap<Integer, Triple> matchNotPrematched(TransitRoute visRoute, TransitRoute hafRoute, SortedMap<Integer, Triple> preMatched){
		SortedMap<Integer, Triple> temp = new TreeMap<Integer, Triple>();
		
		ListIterator<TransitRouteStop> vIt = visRoute.getStops().listIterator(preMatched.firstKey());
		ListIterator<TransitRouteStop> hIt = hafRoute.getStops().listIterator(preMatched.get(preMatched.firstKey()).getP());
		
		Id vis, haf;
		
		int vPos, hPos;
		
		if(vIt.hasPrevious()){
			while(vIt.hasPrevious() && hIt.hasPrevious()){
				vPos = vIt.previousIndex();
				hPos = hIt.previousIndex();
				vis = vIt.previous().getStopFacility().getId();
				haf = hIt.previous().getStopFacility().getId();
				temp.put(vPos, new Triple(hPos, vis, haf));
			}
			if(vIt.hasPrevious()) return null;
		}
		
		while(vIt.hasNext() && hIt.hasNext()){
			vPos = vIt.nextIndex();
			hPos = hIt.nextIndex();
			vis = vIt.next().getStopFacility().getId();
			haf = hIt.next().getStopFacility().getId();
			
			if (preMatched.containsKey(vPos) || temp.containsKey(vPos)) continue;
			
			temp.put(vPos, new Triple(hPos, vis, haf));
		}
		
		if (vIt.hasNext()){
			return null;
		}else{
			return temp;
		}
 	}
	
	
	private SortedMap<Integer, Triple> matchPrematchedStraightOrderRoute(TransitRoute visRoute, TransitRoute hafRoute) {
		SortedMap<Integer, Triple> sortedMatch = new TreeMap<Integer, Triple>();
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
					sortedMatch.put(vIt.previousIndex(), new Triple(hIt.previousIndex(), vStop, hStop));
				}
			}
		}
		return sortedMatch;
	}

	private Map<Id, Id> triple2IdPair(Map<Integer, Triple> sorted){
		Map<Id, Id> matched = new HashMap<Id, Id>();
		
		for(Triple t : sorted.values()){
			matched.put(t.getV(), t.getH());
		}
		
		return matched;
	}
}


class Triple{
	private Integer position;
	private Id v;
	private Id h;
	
	public Triple(int hPosition, Id v, Id h){
		this.h = h;
		this.v = v;
		this.position = hPosition;
	}
	
	public int getP(){
		return this.position;
	}
	
	public Id getV(){
		return this.v;
	}
	
	public Id getH(){
		return this.h;
	}
	
	
}