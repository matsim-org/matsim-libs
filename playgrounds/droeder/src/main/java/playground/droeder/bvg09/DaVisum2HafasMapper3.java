package playground.droeder.bvg09;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class DaVisum2HafasMapper3 extends AbstractDaVisum2HafasMapper {
	
	public static void main(String[] args){
		DaVisum2HafasMapper3 mapper = new DaVisum2HafasMapper3();
		mapper.run();
	}
	public DaVisum2HafasMapper3(){
		super(150.0);
	}

	@Override
	protected Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute) {
		if(hafRoute.getStops().size() < visRoute.getStops().size()) return null;
		
		Map<Id, Id> matchedStops = new HashMap<Id, Id>();
		
		ListIterator<TransitRouteStop> vIt = visRoute.getStops().listIterator();
		ListIterator<TransitRouteStop> hIt;
		TransitStopFacility vStop;
		TransitStopFacility hStop;
		int hItPosition = 0;
		int vItPosition = 0;
		int match = 0;
		
		
		while(match < visRoute.getStops().size() && vIt.hasNext() ){
			vIt = visRoute.getStops().listIterator(vItPosition);
			hIt = hafRoute.getStops().listIterator(hItPosition);

			if (hIt.hasNext() && vIt.hasNext()){
				vStop = vIt.next().getStopFacility();
				hStop = hIt.next().getStopFacility();
			}else{
				return null;
			}
			
			
			boolean matched = false;
			if(this.preVisum2HafasMap.containsKey(vStop.getId()) && this.preVisum2HafasMap.get(vStop.getId()).equals(hStop.getId())){
				// stops prematched, add to matchedMap
				matchedStops.put(vStop.getId(), this.preVisum2HafasMap.get(vStop.getId()));
				match++;
				vItPosition = vIt.previousIndex() + 1;
				hItPosition = hIt.previousIndex() + 1;
				matched = true;
			}else{
				while(hIt.hasNext() && (matched == false)){
					hStop = hIt.next().getStopFacility();
					if(this.preVisum2HafasMap.containsKey(vStop.getId()) && this.preVisum2HafasMap.get(vStop.getId()).equals(hStop.getId())){
						matchedStops.put(vStop.getId(), this.preVisum2HafasMap.get(vStop.getId()));
						vItPosition = vIt.previousIndex() + 1;
						hItPosition = hIt.previousIndex() + 1;
						match++;
						matched = true;
					}
				}
			}
			if(matched == true  &&  match < vItPosition){
				while( match < vItPosition){
					if(hIt.hasPrevious() && vIt.hasPrevious()){
						vStop = vIt.previous().getStopFacility();
						hStop = hIt.previous().getStopFacility();
					}else{
						return null;
					}
					matchedStops.put(vStop.getId(), hStop.getId());
					match++;
				}
			}else if(matched == false){
				vItPosition++;
			}
		}
		if(match == visRoute.getStops().size()){
			return matchedStops;
		}else if((match < visRoute.getStops().size()) && 
				((hafRoute.getStops().size() - hItPosition) <= (visRoute.getStops().size() -vItPosition)) ){
			
			hIt = hafRoute.getStops().listIterator(hItPosition);
			vIt = visRoute.getStops().listIterator(vItPosition);
			while(vIt.hasNext() && hIt.hasNext()){
				vStop = vIt.next().getStopFacility();
				hStop = hIt.next().getStopFacility();
				matchedStops.put(vStop.getId(), hStop.getId());
			}
			
			return matchedStops;
		}else{
			return null;
		}
		
	}

}
