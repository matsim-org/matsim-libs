package playground.johannes.gsv.misc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class TransitScheduleCut {

	public static void main(String[] args) throws IOException, FactoryException, TransformException {
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Set<SimpleFeature> features = FeatureSHP.readFeatures("/home/johannes/gsv/matsim/studies/netz2030/data/raw/Zonierung_Kreise_WGS84_Stand2008Attr_WGS84_region.shp");
		Set<Geometry> geometries = new HashSet<Geometry>();
		for(SimpleFeature feature : features) {
			String code = (String)feature.getAttribute("NUTS0_CODE");
			if(code.equalsIgnoreCase("DE")) {
				geometries.add(((Geometry)feature.getDefaultGeometry()).getGeometryN(0));
			}
		}
		
		MathTransform transform = CRS.findMathTransform(CRSUtils.getCRS(31467), DefaultGeographicCRS.WGS84);
		GeometryFactory geoFactory = new GeometryFactory();
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/network.gk3.xml");
		
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.routed.gk3.xml");
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		Set<TransitLine> remove = new HashSet<TransitLine>();
		for(TransitLine line : schedule.getTransitLines().values()) {
			boolean isInGeometry = false;
			for(TransitRoute route : line.getRoutes().values()) {
				for(TransitRouteStop stop : route.getStops()) {
					Coord c = stop.getStopFacility().getCoord();
					
					double[] points = new double[]{c.getX(), c.getY()};
					transform.transform(points, 0, points, 0, 1);
					
					Point p = geoFactory.createPoint(new Coordinate(points[0], points[1]));
					for (Geometry geometry : geometries) {
						if (geometry.contains(p)) {
							isInGeometry = true;
							break;
						}
					}
				}
			}
			
			if(!isInGeometry) {
				remove.add(line);
			}
		}
		
		Set<TransitStopFacility> stops = new HashSet<TransitStopFacility>();
		System.out.println(String.format("Removing %s of %s transit lines...", remove.size(), schedule.getTransitLines().size()));
		for(TransitLine line : remove) {
			schedule.removeTransitLine(line);
			
			for(TransitRoute route : line.getRoutes().values()) {
				for(TransitRouteStop stop : route.getStops()) {
					stops.add(stop.getStopFacility());
				}
			}
		}
		
		System.out.println(String.format("Removing %s of %s transit stop facilities...", stops.size(), schedule.getFacilities().size()));
		for(TransitStopFacility stop : stops) {
			schedule.removeStopFacility(stop);
		}
		
		TransitScheduleWriter writer = new TransitScheduleWriter(schedule);
		writer.writeFile("/home/johannes/gsv/matsim/studies/netz2030/data/transitSchedule.routed.gk3.de.xml");
	}

}
