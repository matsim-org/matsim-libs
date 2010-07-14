package playground.droeder.bvg09;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;

public class DaViSum2HafasMapper8 extends AbstractDaVisum2HafasMapper {
	
	private final Map<Id, Id> prematched;
	
	List<TransitRouteStop> vStops;
	List<TransitRouteStop> hStops;
	
	public static void main(String[] args) {
		DaViSum2HafasMapper8 mapper = new DaViSum2HafasMapper8(150);
		mapper.run();
//		mapper.analyseUnmatchedRoutes();
	}

	public DaViSum2HafasMapper8(double dist2Match) {
		super(dist2Match);
		prematched = this.getPrematchedStops();
	}
	
	public DaViSum2HafasMapper8(double dist2Match, Map<Id, Id> prematched) {
		super(dist2Match);
		this.prematched = prematched;
	}

	@Override
	protected Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute) {
		if(visRoute == null || hafRoute == null){
			return null;
		}
		
		this.vStops = visRoute.getStops();
		this.hStops = hafRoute.getStops();
		
		
		if(vStops.size() > hStops.size()){
			return null;
		}else if(vStops.size() == hStops.size()){
			return matchSameLength();
		}else if(vStops.size() < hStops.size()){
			return matchShorterVisum();
		}else{
			return null;
		}
		
	}
	
	private Map<Id, Id> matchSameLength(){
		if((this.getDist(vStops.get(0).getStopFacility().getId(), hStops.get(0).getStopFacility().getId()) > 
				this.getDist(vStops.get(0).getStopFacility().getId(), hStops.get(hStops.size() -1).getStopFacility().getId()))){
			return null;
		}
		
		Map<Id, Id> matched = new HashMap<Id, Id>();
		
		Id vis;
		Id haf;
		
		int pre = 0;
		
		for(int i = 0 ; i < vStops.size(); i++){
			vis = vStops.get(i).getStopFacility().getId();
			haf = hStops.get(i).getStopFacility().getId();
			if(prematched.containsKey(vis)){
				pre++;
				if(prematched.get(vis).equals(haf)){
					matched.put(vis, haf);
				}else{
					return null;
				}
			}else{
				matched.put(vis, haf);
			}
		}
		
		return matched;
		
	}
	
	private Map<Id, Id> matchShorterVisum(){
		
		for(TransitRouteStop stop : vStops){
			System.out.print(stop.getStopFacility().getId() + "\t");
		}
		System.out.println();
		for(TransitRouteStop stop : hStops){
			System.out.print(stop.getStopFacility().getId() + "\t");
		}
		System.out.println();
		
		return null;
	}
	
	private void analyseUnmatchedRoutes(){
		
//		for(Entry<Id, Id> e : this.getVis2HafLines().entrySet()){
//			if(this.getUnmatchedLines().contains(e.getKey())){
//				System.out.println("visumLine" + e.getKey());
//				for(TransitRoute vRoute : this.getVisumTransit().getTransitLines().get(e.getKey()).getRoutes().values()){
//					System.out.print("vRouteId:" + vRoute.getId() + "\t");
//					for(TransitRouteStop stop : vRoute.getStops()){
//						System.out.print(stop.getStopFacility().getId() + "\t");
//					}
//					System.out.println();
//				}
//				
//				for(TransitRoute hRoute : this.getHafasTransit().getTransitLines().get(e.getValue()).getRoutes().values()){
//					System.out.print("hRouteId:" + hRoute.getId() + "\t \t");
//					for(TransitRouteStop stop : hRoute.getStops()){
//						System.out.print(stop.getStopFacility().getId() + "\t");
//					}
//					System.out.println();
//				}
//			}
//		}
		
		for(Entry<Id, Map<Id, Id>> e : this.getVisRoute2Vis2HafStops().entrySet()){
			System.out.println(e.getKey());
			for(Id id : e.getValue().keySet()){
				System.out.print(id + "\t");
			}
			System.out.println();
			for(Id id : e.getValue().values()){
				System.out.print(id + "\t");
			}
			System.out.println();
		}
	}
	
}
