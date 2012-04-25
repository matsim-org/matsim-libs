package playground.mzilske.teach;

import java.util.ArrayList;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class WriteAnalysisToShapefile {

	public static void main(String[] args) throws FactoryConfigurationError, SchemaException, IllegalAttributeException {
		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FeatureType featureType = FeatureTypeBuilder.newFeatureType(new AttributeType[] {
				AttributeTypeFactory.newAttributeType("shape", LineString.class),
				AttributeTypeFactory.newAttributeType("link-id", String.class),
				AttributeTypeFactory.newAttributeType("laenge", Double.class)
		}, "link");

		GeometryFactory geometryFactory = new GeometryFactory();

		ArrayList<Feature> features = new ArrayList<Feature>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			System.out.println(link.getLength());
			Feature feature = featureType.create(new Object[] {
				geometryFactory.createLineString(new Coordinate[] {
						new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()),
						new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY())
				}),
				link.getId(),
				link.getLength()
			});
			features.add(feature);
		}

		ShapeFileWriter.writeGeometries(features, "output/kanten");
	}

}
