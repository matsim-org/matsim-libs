package playgroundMeng.ptAccessabilityAnalysis.prepare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.apache.commons.collections.map.HashedMap;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class ShapeFileFilter {
	String fileString;
	Network network;
	Population population;
	//StageActivityTypes stageActivities;
	
	Map<String, List<Id<Link>>> district2LinkId= new HashedMap();
	Map<String, List<Trip>> district2trips = new HashedMap();
	
	public ShapeFileFilter(String fileString, Network network) {
		this.fileString=fileString;
		this.network = network;
	}
//	public ShapeFileFilter(String fileString, Network network, Population population,StageActivityTypes stageActivities) {
//		this.fileString=fileString;
//		this.network = network;
//		this.population = population;
//		this.stageActivities = stageActivities;
//	}
	public void filter(){
		Geometry geometry = null;
		GeometryFactory gf = new GeometryFactory();
		
		ShapeFileReader shapeFileReader = new ShapeFileReader();
		for(SimpleFeature simpleFeature: shapeFileReader.getAllFeatures(this.fileString)) {
			List<Activity> Trips = new ArrayList<>();
			List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
			this.district2LinkId.put(simpleFeature.getAttribute("NAME").toString(), linkIds);
			geometry = (Geometry) simpleFeature.getDefaultGeometry();
			
			for(Link link : network.getLinks().values()) {
				boolean bo = geometry.contains(gf.createPoint(new Coordinate(link.getCoord().getX(),link.getCoord().getY())));
				if(bo){
					linkIds.add(link.getId());
				}
			}
		}
	}
	public Map<String, List<Id<Link>>> getDistrict2LinkId() {
		return district2LinkId;
	}
	public Map<String, List<Trip>> getDistrict2trips() {
		return district2trips;
	}
}
