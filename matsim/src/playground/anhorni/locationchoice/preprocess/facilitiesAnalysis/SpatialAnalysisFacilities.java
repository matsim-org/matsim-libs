package playground.anhorni.locationchoice.preprocess.facilitiesAnalysis;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Facility;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.anhorni.locationchoice.cs.depr.filters.AreaReader;

public class SpatialAnalysisFacilities {

	private final static Logger log = Logger.getLogger(SpatialAnalysisFacilities.class);
	private List<Polygon> area;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpatialAnalysisFacilities analyzer = new SpatialAnalysisFacilities();
		analyzer.run(args[0], args[1]);
	}
	
	public void run(String facilitiesFile, String areaShapeFile) {

		TreeMap<Id,Facility> shop_facilities = this.readAndFilterFacilities(facilitiesFile);
		
		// Read area
		AreaReader areaReader = new AreaReader();
		areaReader.readShapeFile(areaShapeFile);
		this.area = areaReader.getAreaPolygons();
		
		List<Facility> zhShopFacilities = this.filterStores(shop_facilities);
		this.print(zhShopFacilities);
	}
	
	private void print(List<Facility> facilities) {
		log.info(facilities.size());
		FacilitiesWriter writer = new FacilitiesWriter();
		int [] numberOfFacilities = writer.write(facilities);
		
		log.info("Number of shop_retail_gt2500sqm: " + numberOfFacilities[0]);
		log.info("Number of shop_retail_get1000sqm: " + numberOfFacilities[1]);
		log.info("Number of shop_retail_get400sqm: " + numberOfFacilities[2]);
		log.info("Number of shop_retail_get100sqm: " + numberOfFacilities[3]);
		log.info("Number of shop_retail_lt100sqm: " + numberOfFacilities[4]);
		log.info("Number of shop_other: " + numberOfFacilities[5]);	
	}
	
	private TreeMap<Id,Facility> readAndFilterFacilities(String facilitiesFile) {
		Facilities facilities  =(Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(facilities).readFile(facilitiesFile);
		
		TreeMap<Id,Facility> shop_facilities = new TreeMap<Id,Facility>();
		shop_facilities.putAll(facilities.getFacilities("shop_retail_gt2500sqm"));
		shop_facilities.putAll(facilities.getFacilities("shop_retail_get1000sqm"));
		shop_facilities.putAll(facilities.getFacilities("shop_retail_get400sqm"));
		shop_facilities.putAll(facilities.getFacilities("shop_retail_get100sqm"));
		shop_facilities.putAll(facilities.getFacilities("shop_retail_lt100sqm"));
		shop_facilities.putAll(facilities.getFacilities("shop_other"));

		return shop_facilities;
	}
	
	private List<Facility> filterStores(TreeMap<Id,Facility> shop_facilities) {
		
		List<Facility> zhShopFacilities = new Vector<Facility>();
		
		Iterator<Facility> shop_iter = shop_facilities.values().iterator();
		while (shop_iter.hasNext()) {
			Facility facility = shop_iter.next();
			
			if (this.insideArea(facility.getCenter())) {
				zhShopFacilities.add(facility);
			}
		}
		return zhShopFacilities;
	}
	
	private boolean insideArea(Coord coordIn) {
		GeometryFactory geometryFactory = new GeometryFactory();
		Coordinate coord = new Coordinate(coordIn.getX(), coordIn.getY());
		Point point = geometryFactory.createPoint(coord);
		
		Iterator<Polygon> polygon_it = this.area.iterator();
		while (polygon_it.hasNext()) {
			Polygon polygon = polygon_it.next();
			if (polygon.contains(point)) {
				return true;
			}
		}
		return false;
	}
}
