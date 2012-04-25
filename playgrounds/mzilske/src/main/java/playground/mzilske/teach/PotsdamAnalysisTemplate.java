package playground.mzilske.teach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class PotsdamAnalysisTemplate implements Runnable {
	
	private FeatureType featureType;
	
	private GeometryFactory geometryFactory = new GeometryFactory();

	private Scenario scenario;
	
	public static void main(String[] args) {
		PotsdamAnalysisTemplate potsdamAnalysis = new PotsdamAnalysisTemplate();
		potsdamAnalysis.run();
	}

	@Override
	public void run() {
		analyse();
		initFeatureType();
		Collection<Feature> features = createFeatures();
		ShapeFileWriter.writeGeometries(features, "output/links");
	}

	private void analyse() {
		scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig("examples/equil/config.xml"));
	}

	private List<Feature> createFeatures() {
		List<Feature> features = new ArrayList<Feature>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Feature feature = getFeature(link);
			features.add(feature);
		}
		return features;
	}
	
	
	/**
	 * Ein FeatureType ist sozusagen die Datensatzbeschreibung für das, was visualisiert werden soll.
	 * Schreiben Sie in das Array attribs hier Ihre eigenen Datenfelder dazu.
	 * 
	 */
	private void initFeatureType() {
		AttributeType[] attribs = new AttributeType[] {
				AttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S)),
				AttributeTypeFactory.newAttributeType("ID", String.class),
				AttributeTypeFactory.newAttributeType("length", Double.class)
				
				// Hier weitere Attribute anlegen
		};
		
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
		Object [] attribs = new Object[] {
				this.geometryFactory.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())}),
				link.getId().toString(),
				link.getLength()
				
				// Hier die weiteren Attribute ausfüllen
		};
		
		try {
			return this.featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}

	}
	
}
