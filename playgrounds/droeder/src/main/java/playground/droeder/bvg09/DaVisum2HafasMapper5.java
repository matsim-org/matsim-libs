package playground.droeder.bvg09;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class DaVisum2HafasMapper5 extends AbstractDaVisum2HafasMapper {

	public static void main(String[] args) {
		DaVisum2HafasMapper5 mapper = new DaVisum2HafasMapper5(150);
		mapper.run();
	}

	public DaVisum2HafasMapper5(double dist2Match) {
		super(dist2Match);
	}

	@Override
	protected Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute) {
		SortedMap<Integer, Integer> preMatchedStops = this.getPrematchedPosition(visRoute, hafRoute);
		SortedMap<Integer, Integer> matchedStops = this.getPrematchedPosition(visRoute, hafRoute);
		Integer vPos = 0;
		Integer hPos = 0;
		
		for (Entry<Integer, Integer> e : preMatchedStops.entrySet()){
			if (e.getValue() < hPos)return null;
			SortedMap<Integer, Integer> temp = this.matchRouteParts(visRoute.getStops().subList(vPos + 1, e.getKey() -1), 
					hafRoute.getStops().subList(hPos +1, e.getValue() - 1), vPos+1, hPos+1);
			
			if (temp == null){
				return null;
			}else{
				for (Entry<Integer, Integer> ee : temp.entrySet()){
					matchedStops.put(ee.getKey(), ee.getValue());
				}
			}
			
		}
		
		matchedStops.putAll(preMatchedStops);
		
		return null;
	}
	
	private SortedMap<Integer, Integer> matchRouteParts(List<TransitRouteStop> visPart, List<TransitRouteStop> hafPart, Integer vOffset, Integer hOffset ){
		//no matching possible if hafasSubRoute is shorter than visSubroute
		if (hafPart.size() < visPart.size()) return null;
		
		SortedMap<Integer, Integer> temp = new TreeMap<Integer, Integer>();
		
		List<Map<Integer, Integer>>	solutions = new ArrayList<Map<Integer,Integer>>();	

		int hStart = 0;
		for(int v = 0; v < visPart.size(); v++){
			
			
			hStart++;
		}
		
		
		return temp;
	}
	
	private SortedMap<Integer, Integer> getPrematchedPosition(TransitRoute visRoute, TransitRoute hafRoute) {
		Id vis, haf;
		SortedMap<Integer, Integer> position = null;
		
		
		for(int i = 0; i < visRoute.getStops().size(); i++){
			vis = visRoute.getStops().get(i).getStopFacility().getId();
			if(this.preVisum2HafasMap.containsKey(vis)){
				for(int j = 0; j < hafRoute.getStops().size(); j++){
					haf = hafRoute.getStops().get(j).getStopFacility().getId();
					if(this.preVisum2HafasMap.get(vis).equals(haf)){
						if(position == null){
							position = new TreeMap<Integer, Integer>();
						}
						if(position.containsKey(i) || position.containsValue(j)) return null;
						position.put(i, j);
						break;
					}
					
				}
			}
			
		}
		
		
		return position;
	}

}
