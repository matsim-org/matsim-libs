package playground.droeder.bvg09.Visum2HafasMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.droeder.DaPaths;
import playground.droeder.gis.DaShapeWriter;

public class DaViSum2HafasMapper8 extends AbstractDaVisum2HafasMapper {
	
	private final Map<Id, Id> prematched;
	
	List<TransitRouteStop> vStops;
	List<TransitRouteStop> hStops;
	
	public static void main(String[] args) {
		DaViSum2HafasMapper8 mapper = new DaViSum2HafasMapper8(150);
		mapper.run();
		mapper.matchedStopsDist2Shape();
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
		
		/*
		 * check if the distance of order is correct. uses coordinates. might not be correct due to the fact that the coordinates of
		 * visum and hafas are not always correct.
		 */
		if((this.getDist(vStops.get(0).getStopFacility().getId(), hStops.get(0).getStopFacility().getId()) > 
				this.getDist(vStops.get(0).getStopFacility().getId(), hStops.get(hStops.size() -1).getStopFacility().getId()))){
			return null;
		}
		
		Map<Id, Id> matched = new HashMap<Id, Id>();
		
		Id vis;
		Id haf;
		
		int pre = 0;
		
		/*
		 * checks if the visumstop is  prematched. if so check if the hafasstop at position i is equivalent to the prematched hafasstop.
		 * 			adds the id to matched or returns null
		 * if the stop is not prematched add visum and hafas from position i
		 */
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
				if(getDist(vis, haf) > 2* distToMatch){
					return null;
				}else{
					matched.put(vis, haf);
				}
			}
		}
		
		if(getAvDist(matched) > distToMatch){
			return null;
		}else{
			return matched;
		}
	}
	
	private Map<Id, Id> matchShorterVisum(){
		/*
		 * not implemented yet
		 */
		
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
	
	private void matchedStopsDist2Shape(){
		Map<String, Tuple<Coord, Coord>> points = new HashMap<String, Tuple<Coord,Coord>>();
		Map<String, SortedMap<String, String>> attribs = new HashMap<String, SortedMap<String,String>>();
		
		String name;
		String wasPrematchedTo;
		
		
		for(Entry<Id, Map<Id, Id>> m : this.getVisRoute2Vis2HafStops().entrySet()){
			for(Entry<Id, Id> e : m.getValue().entrySet()){
				name = e.getKey() + "_" + e.getValue();
				if(prematched.containsKey(e.getKey())){
					wasPrematchedTo = prematched.get(e.getKey()).toString();
				}else{
					wasPrematchedTo = "null";
				}
				
				points.put(name, new Tuple<Coord, Coord>(this.getVisumTransit().getFacilities().get(e.getKey()).getCoord(), 
						this.getHafasTransit().getFacilities().get(e.getValue()).getCoord()));
				SortedMap<String, String> tmp = new TreeMap<String, String>();
				tmp.put("prematched", wasPrematchedTo);
				tmp.put("onLine", m.getKey().toString());
				attribs.put(name, tmp);
			}
		}
		
		DaShapeWriter.writePointDist2Shape(DaPaths.OUTPUT + "bvg09/matchedDist.shp", points, attribs);
	}
	
}
