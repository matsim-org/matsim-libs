package playground.andreas.bln.ana;

import java.util.HashMap;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class CountVehOnLinksPolygonBasedFeatureGenerator implements FeatureGenerator{

	private static final double PI_HALF = Math.PI / 2.0;
	private static final double TWO_PI = 2.0 * Math.PI;

	private final WidthCalculator widthCalculator;
	private final CoordinateReferenceSystem crs;
	private final GeometryFactory geofac;
	private FeatureType featureType;
	private final HashMap<String, Integer> compareResultMap;


	public CountVehOnLinksPolygonBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.geofac = new GeometryFactory();
		initFeatureType();

		this.compareResultMap = CountVehOnLinks.compareEventFiles("c:\\Users\\aneumann\\Documents\\VSP_Extern\\Berlin\\berlin-sharedsvn\\network\\A100\\763.500.events.txt", "c:\\Users\\aneumann\\Documents\\VSP_Extern\\Berlin\\berlin-sharedsvn\\network\\A100\\762.500.events.txt");
	}

	private void initFeatureType() {

		AttributeType [] attribs = new AttributeType[10];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, this.crs);
		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("fromID", String.class);
		attribs[3] = AttributeTypeFactory.newAttributeType("toID", String.class);
		attribs[4] = AttributeTypeFactory.newAttributeType("length", Double.class);
		attribs[5] = AttributeTypeFactory.newAttributeType("freespeed", Double.class);
		attribs[6] = AttributeTypeFactory.newAttributeType("capacity", Double.class);
		attribs[7] = AttributeTypeFactory.newAttributeType("lanes", Double.class);
		attribs[8] = AttributeTypeFactory.newAttributeType("visWidth", Double.class);
		attribs[9] = AttributeTypeFactory.newAttributeType("Diff", Double.class);

		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}

	}


	@Override
	public Feature getFeature(final Link link) {
		double width = this.widthCalculator.getWidth(link);
		width += 0;

		Coordinate from = MGC.coord2Coordinate(link.getFromNode().getCoord());
		Coordinate to = MGC.coord2Coordinate(link.getToNode().getCoord());
		double length = from.distance(to);

		final double dx = -from.x   + to.x;
		final double dy = -from.y   + to.y;

		double theta = 0.0;
		if (dx > 0) {
			theta = Math.atan(dy/dx);
		} else if (dx < 0) {
			theta = Math.PI + Math.atan(dy/dx);
		} else { // i.e. DX==0
			if (dy > 0) {
				theta = PI_HALF;
			} else {
				theta = -PI_HALF;
			}
		}
		if (theta < 0.0) theta += TWO_PI;
		double xfrom2 = from.x + Math.cos(theta) * 0  + Math.sin(theta) * width;
		double yfrom2 = from.y + Math.sin(theta) * 0 - Math.cos(theta) * width;
		double xto2 = from.x + Math.cos(theta) *  length + Math.sin(theta) * width;
		double yto2 = from.y + Math.sin(theta) * length - Math.cos(theta) * width;
		Coordinate from2 = new Coordinate(xfrom2,yfrom2);
		Coordinate to2 = new Coordinate(xto2,yto2);

		Polygon p = this.geofac.createPolygon(this.geofac.createLinearRing(new Coordinate[] {from, to, to2, from2, from}), null);
		Object [] attribs = new Object[10];
		attribs[0] = p;
		attribs[1] = link.getId().toString();
		attribs[2] = link.getFromNode().getId().toString();
		attribs[3] = link.getToNode().getId().toString();
		attribs[4] = link.getLength();
		attribs[5] = link.getFreespeed();
		attribs[6] = link.getCapacity();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = width;

		if(this.compareResultMap.get(link.getId().toString()) != null){
			attribs[9] = this.compareResultMap.get(link.getId().toString());
		}

		try {
			return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}

	}

}
