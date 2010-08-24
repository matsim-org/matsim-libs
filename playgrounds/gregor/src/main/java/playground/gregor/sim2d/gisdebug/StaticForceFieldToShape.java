package playground.gregor.sim2d.gisdebug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.sim2d.simulation.Force;
import playground.gregor.sim2d.simulation.StaticForceField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class StaticForceFieldToShape {

	private FeatureType ft;
	private FeatureType ftLine;
	private FeatureType ftPoint;
	private final StaticForceField forces;

	double downScale = 100;

	public StaticForceFieldToShape(StaticForceField sff) {
		this.forces = sff;
	}
	
	public void createShp() {
		initFeatures();
		List<Coordinate[]> coords = new ArrayList<Coordinate[]>();
		for (Force f : this.forces.getForces()) {
			
			Coordinate [] coord = {new Coordinate(f.getXCoord(),f.getYCoord()),new Coordinate(f.getXCoord()+f.getFx()/this.downScale,f.getYCoord()+f.getFy()/this.downScale),new Coordinate(f.getXCoord()+f.getFx()/this.downScale+0.01,f.getYCoord()+f.getFy()/this.downScale+0.01)};
			coords.add(coord);
		}
		dump(coords);
		
	}
	
	private void dump(List<Coordinate[] > coords) {
		initFeatures();		
		List<Feature> fts = new ArrayList<Feature>();
		GeometryFactory geofac = new GeometryFactory();
		for (Coordinate [] coord : coords) {
			LineString ls = geofac.createLineString(coord);
			try {
				fts.add(this.ftLine.create(new Object [] {ls,0,0}));
			} catch (IllegalAttributeException e) {
				e.printStackTrace();
			}
		}

		try {
			ShapeFileWriter.writeGeometries(fts, "../../../../tmp/staticForces.shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType(
				"MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
		AttributeType l = DefaultAttributeTypeFactory.newAttributeType(
				"LineString", LineString.class, true, null, null, targetCRS);
		AttributeType po = DefaultAttributeTypeFactory.newAttributeType(
				"Point", Point.class, true, null, null, targetCRS);
		AttributeType z = AttributeTypeFactory.newAttributeType(
				"dblAvgZ", Double.class);
		AttributeType t = AttributeTypeFactory.newAttributeType(
				"dblAvgT", Double.class);

		Exception ex;
		try {
			this.ft = FeatureTypeFactory.newFeatureType(new AttributeType[] { p, z, t }, "Polygon");
			this.ftLine = FeatureTypeFactory.newFeatureType(new AttributeType[] { l, z, t }, "Line");
			this.ftPoint = FeatureTypeFactory.newFeatureType(new AttributeType[] { po, z, t }, "Point");
			return;
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}
}
