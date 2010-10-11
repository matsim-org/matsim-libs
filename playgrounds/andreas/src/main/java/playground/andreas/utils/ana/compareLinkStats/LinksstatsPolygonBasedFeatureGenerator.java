package playground.andreas.utils.ana.compareLinkStats;

import java.util.ArrayList;
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

public class LinksstatsPolygonBasedFeatureGenerator implements FeatureGenerator{

	private static final double PI_HALF = Math.PI / 2.0;
	private static final double TWO_PI = 2.0 * Math.PI;

	private final WidthCalculator widthCalculator;
	private final CoordinateReferenceSystem crs;
	private final GeometryFactory geofac;
	private FeatureType featureType;
	private final HashMap<String, ArrayList<Double>> compareResultMap;


	public LinksstatsPolygonBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.geofac = new GeometryFactory();
		initFeatureType();

		this.compareResultMap = EvaluateLinkstats.compareLinkstatFiles(LinkStatsCompareConfig.linkStatsFileOne, LinkStatsCompareConfig.linkStatsFileTwo);
	}

	private void initFeatureType() {

		AttributeType [] attribs = new AttributeType[9+8+24];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, this.crs);
		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("fromID", String.class);
		attribs[3] = AttributeTypeFactory.newAttributeType("toID", String.class);
		attribs[4] = AttributeTypeFactory.newAttributeType("length", Double.class);
		attribs[5] = AttributeTypeFactory.newAttributeType("freespeed", Double.class);
		attribs[6] = AttributeTypeFactory.newAttributeType("capacity", Double.class);
		attribs[7] = AttributeTypeFactory.newAttributeType("lanes", Double.class);
		attribs[8] = AttributeTypeFactory.newAttributeType("visWidth", Double.class);

		for (int i = 0; i < 8; i++) {
			attribs[9 + i] = AttributeTypeFactory.newAttributeType("HRS" + (i + i * 2) + "-" + (i + i * 2 + 3) + "avg", Double.class);
		}

		for (int i = 0; i < 24; i++) {
			attribs[9 + 8 + i] = AttributeTypeFactory.newAttributeType("HRS" + i + "-" + (i+1) + "avg", Double.class);
		}

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
		Object [] attribs = new Object[9+8+24];
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
			ArrayList<Double> tempArray = this.compareResultMap.get(link.getId().toString());
			for (int i = 0; i < 8; i++) {
				attribs[9 + i] = Double.valueOf((tempArray.get(i + i * 2).doubleValue() + tempArray.get(i + 1 + i * 2).doubleValue() + tempArray.get(i + 2 + i * 2).doubleValue()) / 3);
			}
		}

		if(this.compareResultMap.get(link.getId().toString()) != null){
			ArrayList<Double> tempArray = this.compareResultMap.get(link.getId().toString());
			for (int i = 0; i < tempArray.size(); i++) {
				attribs[9 + 8 + i] = tempArray.get(i);
			}
		}

		try {
			return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}

	}

}
