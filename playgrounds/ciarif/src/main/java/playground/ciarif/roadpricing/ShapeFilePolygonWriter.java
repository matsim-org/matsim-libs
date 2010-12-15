package playground.ciarif.roadpricing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.feature.AttributeType;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.DefaultFeatureTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class ShapeFilePolygonWriter {
	protected GeometryFactory geofac;
	protected DefaultFeatureTypeFactory dftf;
	private CoordinateReferenceSystem crs;
	protected List<AttributeType> attrTypes = new ArrayList<AttributeType>();


	public ShapeFilePolygonWriter() {
		this.geofac = new GeometryFactory();
	}


	public void writePolygon(Coord[] coords, String outfile) {

		Collection<Feature> features = getFeature(getLinearRing(coords));


		try {
			ShapeFileWriter.writeGeometries(features, outfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public LinearRing getLinearRing(Coord[] coords) {
		Coordinate[] coordinates = new Coordinate[coords.length];
		for (int i = 0; i < coords.length; i++) {
			coordinates[i] = new Coordinate(coords[i].getX(), coords[i].getY());
		}
		return new LinearRing(new CoordinateArraySequence(coordinates), this.geofac);
	}

	public Collection<Feature> getFeature(LinearRing ring) {
		this.dftf = new DefaultFeatureTypeFactory();
		AttributeType geom = DefaultAttributeTypeFactory.newAttributeType(
				"LineString", LineString.class, true, null, null, this.crs);
		dftf.addTypes(new AttributeType[] { geom });
		AttributeType multiPolygonType = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, this.crs);


		Polygon p = new Polygon(ring, null, this.geofac);
		MultiPolygon mp = new MultiPolygon(new Polygon[] { p }, this.geofac);
		Collection<Feature> features = new ArrayList<Feature>();
		Object[] o = new Object[1];
		o[0] = mp;
		try {
			for (int i = 0; i < attrTypes.size(); i++) {
				dftf.addType(attrTypes.get(i));
		  }
			FeatureType featureType = dftf.newFeatureType(new AttributeType[] {multiPolygonType}, "polygon");
			Feature f = featureType.create(o, "polygon");
			features.add(f);
			return features;
		} catch (SchemaException e) {
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		return null;
	}
}
