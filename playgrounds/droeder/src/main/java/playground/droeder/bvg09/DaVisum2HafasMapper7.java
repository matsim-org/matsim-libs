package playground.droeder.bvg09;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;

public class DaVisum2HafasMapper7 extends AbstractDaVisum2HafasMapper {

	private final Id TOMATCH = new IdImpl("TOMATCH");
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
			vPos = e.getKey()-1;
			hPos = e.getValue()-1;
		}
		
		temp.putAll(prematched);
		return temp;
	}
	
	private Map<Integer, Integer> matchRouteSegment(int vStart, int vEnd, int hStart, int hEnd, TransitRoute visRoute, TransitRoute hafRoute){
		
		
		return null;
	}

	private Map<Integer, Integer> getPreMatched(TransitRoute visRoute, TransitRoute hafRoute) {
		Map<Integer, Integer> prematched = new HashMap<Integer, Integer>();
		
		log.info("get prematched stops for visRoute " + visRoute.getId() + " and hafas route " + hafRoute.getId());
		Integer ivPos = 0;
		for(TransitRouteStop stop :  visRoute.getStops()){
			Id temp = stop.getStopFacility().getId();
			
			if(this.preVisum2HafasMap.containsKey(temp) ){
				boolean contains = false;
				Integer hPos = -1;
				
				//check if hafas route contains prematched visum stop
				for(TransitRouteStop s : hafRoute.getStops()){
					hPos++;
					if(s.getStopFacility().getId().equals(this.preVisum2HafasMap.get(temp))){
						contains = true;
						break;
					}
				}
				if (contains){
					prematched.put(ivPos, hPos);
					log.info("found prematched Stop for visum Stop " + temp + " on visum position " + 
							ivPos + ". matched to hafas " + this.preVisum2HafasMap.get(temp) + " on Position " + hPos +"!");
				}else{
					log.error("stop " + temp + " is prematched to hafas " + this.preVisum2HafasMap.get(temp) + ", but this stop is not in the hasfas route!");
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
