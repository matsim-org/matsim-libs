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
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.FeatureGenerator;
import org.matsim.utils.gis.matsim2esri.network.WidthCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class CountVehOnLinksStringBasedFeatureGenerator implements FeatureGenerator{


	private final WidthCalculator widthCalculator;
	private FeatureType featureType;
	private final CoordinateReferenceSystem crs;
	private final GeometryFactory geofac;
	private final HashMap<String, Integer> compareResultMap;


	public CountVehOnLinksStringBasedFeatureGenerator(final WidthCalculator widthCalculator, final CoordinateReferenceSystem crs) {
		this.widthCalculator = widthCalculator;
		this.crs = crs;
		this.geofac = new GeometryFactory();
		initFeatureType();
		
		this.compareResultMap = CountVehOnLinks.compareEventFiles("c:\\Users\\aneumann\\Documents\\VSP_Extern\\Berlin\\berlin-sharedsvn\\network\\A100\\763.500.events.txt", "c:\\Users\\aneumann\\Documents\\VSP_Extern\\Berlin\\berlin-sharedsvn\\network\\A100\\762.500.events.txt");
		
	}


	private void initFeatureType() {

		AttributeType [] attribs = new AttributeType[11];
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
		attribs[10] = AttributeTypeFactory.newAttributeType("Diff", Double.class);	

		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		
		

	}


	public Feature getFeature(final LinkImpl link) {
		double width = this.widthCalculator.getWidth(link);
		LineString ls = this.geofac.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),
				MGC.coord2Coordinate(link.getToNode().getCoord())});

		Object [] attribs = new Object[11];
		attribs[0] = ls;
		attribs[1] = link.getId().toString();
		attribs[2] = link.getFromNode().getId().toString();
		attribs[3] = link.getToNode().getId().toString();
		attribs[4] = link.getLength();
		attribs[5] = link.getFreespeed(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
		attribs[6] = link.getCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
		attribs[7] = link.getNumberOfLanes(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
		attribs[8] = width;
		attribs[9] = link.getType();
		
		if(this.compareResultMap.get(link.getId().toString()) != null){
			attribs[10] = this.compareResultMap.get(link.getId().toString());
		}

		try {
			return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}

	}

}
