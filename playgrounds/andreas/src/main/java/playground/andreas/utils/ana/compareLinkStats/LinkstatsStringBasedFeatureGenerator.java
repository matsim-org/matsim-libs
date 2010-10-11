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
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class LinkstatsStringBasedFeatureGenerator implements FeatureGenerator{


	private final WidthCalculator widthCalculator;
	private FeatureType featureType;
	private final CoordinateReferenceSystem crs;
	private final GeometryFactory geofac;
	private final HashMap<String, ArrayList<Double>> compareResultMap;


	public LinkstatsStringBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.geofac = new GeometryFactory();
		initFeatureType();

		this.compareResultMap = EvaluateLinkstats.compareLinkstatFiles(LinkStatsCompareConfig.linkStatsFileOne, LinkStatsCompareConfig.linkStatsFileTwo);
	}


	private void initFeatureType() {

		AttributeType [] attribs = new AttributeType[10+8+24];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, this.crs);
		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("fromID", String.class);
		attribs[3] = AttributeTypeFactory.newAttributeType("toID", String.class);
		attribs[4] = AttributeTypeFactory.newAttributeType("length", Double.class);
		attribs[5] = AttributeTypeFactory.newAttributeType("freespeed", Double.class);
		attribs[6] = AttributeTypeFactory.newAttributeType("capacity", Double.class);
		attribs[7] = AttributeTypeFactory.newAttributeType("lanes", Double.class);
		attribs[8] = AttributeTypeFactory.newAttributeType("visWidth", Double.class);
		attribs[9] = AttributeTypeFactory.newAttributeType("type", String.class);

		for (int i = 0; i < 8; i++) {
			attribs[10 + i] = AttributeTypeFactory.newAttributeType("HRS" + (i + i * 2) + "-" + (i + i * 2 + 3) + "avg", Double.class);
		}

		for (int i = 0; i < 24; i++) {
			attribs[10 + 8 + i] = AttributeTypeFactory.newAttributeType("HRS" + i + "-" + (i+1) + "avg", Double.class);
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
		LineString ls = this.geofac.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),
				MGC.coord2Coordinate(link.getToNode().getCoord())});

		Object [] attribs = new Object[10+8+24];
		attribs[0] = ls;
		attribs[1] = link.getId().toString();
		attribs[2] = link.getFromNode().getId().toString();
		attribs[3] = link.getToNode().getId().toString();
		attribs[4] = link.getLength();
		attribs[5] = link.getFreespeed();
		attribs[6] = link.getCapacity();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = width;
		attribs[9] = ((LinkImpl) link).getType();

		if(this.compareResultMap.get(link.getId().toString()) != null){
			ArrayList<Double> tempArray = this.compareResultMap.get(link.getId().toString());
			for (int i = 0; i < 8; i++) {
				attribs[10 + i] = Double.valueOf((tempArray.get(i + i * 2).doubleValue() + tempArray.get(i + 1 + i * 2).doubleValue() + tempArray.get(i + 2 + i * 2).doubleValue()) / 3);
			}
		}

		if(this.compareResultMap.get(link.getId().toString()) != null){
			ArrayList<Double> tempArray = this.compareResultMap.get(link.getId().toString());
			for (int i = 0; i < tempArray.size(); i++) {
				attribs[10 + 8 + i] = tempArray.get(i);
			}
		}

		try {
			return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}

	}

}
