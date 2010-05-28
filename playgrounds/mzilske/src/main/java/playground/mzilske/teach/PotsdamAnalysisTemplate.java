package playground.mzilske.teach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class PotsdamAnalysisTemplate implements Runnable {
	
	private FeatureType featureType;
	
	private GeometryFactory geometryFactory = new GeometryFactory();
	
	public static void main(String[] args) {
		PotsdamAnalysisTemplate potsdamAnalysis = new PotsdamAnalysisTemplate();
		potsdamAnalysis.run();
	}

	@Override
	public void run() {
		analyse();
		initFeatureType();
		Collection<Feature> features = createFeatures();
		try {
			ShapeFileWriter.writeGeometries(features, "..."); // Hier Dateinamen einfügen
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	private void analyse() {
		// Hier etwas ausrechnen, was dann in die Features geschrieben werden soll.
	}

	private ArrayList<Feature> createFeatures() {
		// Hier Features erzeugen
		return null;
	}
	
	
	/**
	 * Ein FeatureType ist sozusagen die Datensatzbeschreibung für das, was visualisiert werden soll.
	 * Schreiben Sie in das Array attribs hier Ihre eigenen Datenfelder dazu.
	 * 
	 */
	private void initFeatureType() {
		AttributeType [] attribs = new AttributeType[3];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		
		// Hier weitere Attribute anlegen
		
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 
	 * So erzeugt man ein Feature aus einem Link.
	 * Es kann um weitere Attribute erweitert werden.
	 * 
	 */
	private Feature getFeature(final Link link) {
		LineString ls = this.geometryFactory.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())});
		Object [] attribs = new Object[3];
		attribs[0] = ls;
		attribs[1] = link.getId().toString();
		
		// Hier die weiteren Attribute ausfüllen
		
		try {
			return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}

	}
	
}
