package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import playground.anhorni.choiceSetGeneration.filters.AreaReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ZHFilter {
	
	private final static Logger log = Logger.getLogger(ZHFilter.class);
	List<Polygon> areaPolygons = null;
	
	public ZHFilter(String shapeFile) {
		this.readShapeFile(shapeFile);
	}
	
	private void readShapeFile(String shapeFile) {	
		if (shapeFile != null) {
			AreaReader reader = new AreaReader();
			reader.readShapeFile(shapeFile);
			this.areaPolygons = reader.getAreaPolygons();
			log.info("Number of polygons: " + this.areaPolygons.size());
		}
	}
	public List<Hectare> filterHectares(List<Hectare> hectares) {
		
		List<Hectare> filteredHectares = new Vector<Hectare>(); 
		Iterator<Hectare> hectares_it = hectares.iterator();
		while (hectares_it.hasNext()) {
			Hectare hectare = hectares_it.next();
			if (this.withinArea(hectare.getCoords())) {
				filteredHectares.add(hectare);
			}
		}
		return filteredHectares;
	}
	
	public List<ZHFacilityComposed> filterFacilities(List<ZHFacilityComposed> facilities) {
		
		Vector<ZHFacilityComposed> filteredFacilities = new Vector<ZHFacilityComposed>(); 
		Iterator<ZHFacilityComposed> facilities_it = facilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacilityComposed facility = facilities_it.next();
			if (this.withinArea(facility.getCoords())) {
				filteredFacilities.add(facility);
			}
		}
		return filteredFacilities;
	}
	
	private boolean withinArea(Coord coords) {
		GeometryFactory geometryFactory = new GeometryFactory();			
		Coordinate coord = new Coordinate(coords.getX(), coords.getY());
		Point point = geometryFactory.createPoint(coord);
		
		Iterator<Polygon> polygon_it = areaPolygons.iterator();
		while (polygon_it.hasNext()) {
			Polygon polygon = polygon_it.next();
			if (polygon.contains(point)) {
				return true;	
			}
		}
		return false;
	}
}
