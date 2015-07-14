package others.sergioo.shapeFiles;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

public class BusLines {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		new MatsimNetworkReader(scenario).readFile(args[0]);
		new TransitScheduleReader(scenario).readFile(args[1]);
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		PolylineFeatureFactory.Builder b = new PolylineFeatureFactory.Builder();
		b.setCrs(DefaultGeographicCRS.WGS84);
		PolylineFeatureFactory polylineFeatureFactory = b.create();
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM48N, TransformationFactory.WGS84);
		for(TransitLine line:scenario.getTransitSchedule().getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				Coordinate[] points = new Coordinate[route.getRoute().getLinkIds().size()+3];
				Link first = scenario.getNetwork().getLinks().get(route.getRoute().getStartLinkId());
				points[0] = new Coordinate(transformation.transform(first.getFromNode().getCoord()).getX(), transformation.transform(first.getFromNode().getCoord()).getY());
				points[1] = new Coordinate(transformation.transform(first.getToNode().getCoord()).getX(), transformation.transform(first.getToNode().getCoord()).getY());
				int i=1;
				for(Id<Link> linkId:route.getRoute().getLinkIds()) {
					first = scenario.getNetwork().getLinks().get(linkId);
					points[++i] = new Coordinate(transformation.transform(first.getToNode().getCoord()).getX(), transformation.transform(first.getToNode().getCoord()).getY());
				}
				first = scenario.getNetwork().getLinks().get(route.getRoute().getEndLinkId());
				points[++i] = new Coordinate(transformation.transform(first.getToNode().getCoord()).getX(), transformation.transform(first.getToNode().getCoord()).getY());
				SimpleFeature feature = polylineFeatureFactory.createPolyline(points);
				feature.setAttribute("name", route.getId().toString());
				features.add(feature);
			}
		ShapeFileWriter.writeGeometries(features, args[2]);
		PointFeatureFactory.Builder b2 = new PointFeatureFactory.Builder();
		b2.setCrs(DefaultGeographicCRS.WGS84);
		PointFeatureFactory pointFeatureFactory = b2.create();
		Collection<SimpleFeature> features2 = new ArrayList<SimpleFeature>();
		for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values()) {
			SimpleFeature feature =pointFeatureFactory.createPoint(new Coordinate(transformation.transform(stop.getCoord()).getX(), transformation.transform(stop.getCoord()).getY()));
			feature.setAttribute("name", stop.getId().toString());
			features2.add(feature);
		}
		ShapeFileWriter.writeGeometries(features2, args[3]);
	}

}
