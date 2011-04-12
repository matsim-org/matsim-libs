package playground.droeder.bvg09.Visum2HafasMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class DaVisum2HafasMapper7 extends AbstractDaVisum2HafasMapper {

	private final Id TOMATCH = new IdImpl("TOMATCH");
	private final Map<Id, Id> prematched;
	
	public static void main(String[] args) {
		AbstractDaVisum2HafasMapper mapper = new DaVisum2HafasMapper7(150);
		mapper.run();
	}
	
//	@Override
//	public void run(){
//		super.run();
//	}

	public DaVisum2HafasMapper7(double dist2Match) {
		super(dist2Match);
		this.prematched = this.getPrematchedStops();
	}

	@Override
	protected Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute) {
		Map<Integer, Integer> matched = getPreMatched(visRoute, hafRoute);
		
		if (matched == null) {
			return null;
		}else if(matched.size() == visRoute.getStops().size()){
			return this.position2Id(matched, visRoute, hafRoute);
		}else{
			matched = completeMatching(matched, visRoute, hafRoute);
			return this.position2Id(matched, visRoute, hafRoute);
		}
	}
	
	private Map<Integer, Integer> completeMatching(Map<Integer, Integer> prematched, TransitRoute visRoute, TransitRoute hafRoute){
		Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
		
		Integer vPos = 0;
		Integer hPos = 0;
		
		for(Entry<Integer, Integer> e : prematched.entrySet()){
			if (e.getKey() == vPos && e.getValue() == hPos){
				continue;
			}else{
				Map<Integer, Integer> routeSegment = matchRouteSegment(vPos, e.getKey(), hPos, e.getValue(), visRoute, hafRoute);
				if(routeSegment == null){
					return null;
				}else{
					temp.putAll(routeSegment);
				}
			}
			vPos = e.getKey()+1;
			hPos = e.getValue()+1;
		}
		
		temp.putAll(prematched);
		return temp;
	}
	
	private Map<Integer, Integer> matchRouteSegment(int vStart, int vEnd, int hStart, int hEnd, TransitRoute visRoute, TransitRoute hafRoute){
		
		// if the hafasSegment is shorter than the visumSegment return null
		if (((vEnd -  vStart) > (hEnd - hStart)) || (hStart <vStart) ) return null;
		
		// generate Solution
		Map<Integer, List<Integer>> possibleValues = new HashMap<Integer, List<Integer>>();
		for(int i = vStart; i < vEnd; i++){
			List<Integer> temp = new ArrayList<Integer>();
			for (int h = hStart; h < hEnd; h++){
				temp.add(h);
			}
			possibleValues.put(i, temp);
		}
		
		HashMap<Integer, List<Integer>> solutions = new HashMap<Integer, List<Integer>>();
		Integer count = 0;
		
		
		for(Entry<Integer, List<Integer>> e: possibleValues.entrySet()){
			
			// add old solutions to tempMap
			Map<Integer, List<Integer>> temp = new HashMap<Integer, List<Integer>>(solutions);
			//initialize solutions
			solutions = new HashMap<Integer, List<Integer>>();
			count = 0;
			if(e.getKey() == vStart){
				for(Integer i : e.getValue()){
					List<Integer> l = new ArrayList<Integer>();
					l.add(i);
					solutions.put(count, l);
					count++;
				}
			}else{
				// iterate over old solutions
				for (List<Integer> l : temp.values()){
					
					// iterate over possible  values for position e.getkey()
					for (Integer i : e.getValue()){
						
						// get new solution from old
						if(l.size() == (e.getKey()-vStart) && l.get(l.size()-1) < i){
							List<Integer> s = new ArrayList<Integer>(l);
							s.add(i);
							solutions.put(count, s);
							count++;
						}
					}
				}
			}
		}
		System.out.println(vStart + "\t" + vEnd + " +++");
		System.out.println(hStart + "\t" + hEnd + " +++ ");
		for(List<Integer> l : solutions.values()){
			for (Integer i : l){
				System.out.print(i +"\t");
			}
			System.out.println();
		} 
		
		return null;
	}

	private Map<Integer, Integer> getPreMatched(TransitRoute visRoute, TransitRoute hafRoute) {
		Map<Integer, Integer> prematched = new HashMap<Integer, Integer>();
		
		log.info("get prematched stops for visRoute " + visRoute.getId() + " and hafas route " + hafRoute.getId());
		Integer ivPos = 0;
		for(TransitRouteStop stop :  visRoute.getStops()){
			Id temp = stop.getStopFacility().getId();
			
			if(this.prematched.containsKey(temp) ){
				boolean contains = false;
				Integer hPos = -1;
				
				//check if hafas route contains prematched visum stop
				for(TransitRouteStop s : hafRoute.getStops()){
					hPos++;
					if(s.getStopFacility().getId().equals(this.prematched.get(temp))){
						contains = true;
						break;
					}
				}
				if (contains){
					prematched.put(ivPos, hPos);
					log.info("found prematched Stop for visum Stop " + temp + " on visum position " + 
							ivPos + ". matched to hafas " + this.prematched.get(temp) + " on Position " + hPos +"!");
				}else{
					log.error("stop " + temp + " is prematched to hafas " + this.prematched.get(temp) + ", but this stop is not in the hasfas route!");
					return null;
				}
				
			}else{
				log.warn("stop " + temp + " on position " + ivPos +" not prematched! postprocessing necessary!");
			}
			ivPos++;
		}
		
		for(Entry<Integer, Integer> e : prematched.entrySet()){
			log.info(visRoute.getStops().get(e.getKey()).getStopFacility().getId() + " " + hafRoute.getStops().get(e.getValue()).getStopFacility().getId());
		}
		if(prematched.size() < 1){
			return null;
		}else{
			return prematched;
		}
	}


}
