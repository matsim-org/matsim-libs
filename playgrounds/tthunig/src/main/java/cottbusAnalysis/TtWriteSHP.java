package cottbusAnalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * 
 * @author tthunig
 * @deprecated
 */
public class TtWriteSHP {

	private SimpleFeatureType featureType;
	private GeometryFactory geometryFactory = new GeometryFactory();

	private Scenario scenario;
	private TtDetermineComRoutes routesHandler;
	private TtCalculateComTravelTimes travelTimesHandler;
	
//	private CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
//	private CoordinateTransformation ct = TransformationFactory
//			.getCoordinateTransformation("EPSG:3395", crs.toString());
	
	public void writeComRoutes(TtDetermineComRoutes routesHandler, 
			TtCalculateComTravelTimes travelTimesHandler, String networkFile, 
			String plansFile, String outputFile) {
			
		this.routesHandler = routesHandler;
		this.travelTimesHandler = travelTimesHandler;
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(plansFile);
		this.scenario = ScenarioUtils.loadScenario(config);
		
		initFeatureType();
		Collection<SimpleFeature> features = createFeatures();
		ShapeFileWriter.writeGeometries(features, outputFile);
	}
	
	private void initFeatureType() {

		SimpleFeatureType type;
		try {
			type = DataUtilities.createType("Route", "personID:String, comID:String, route:MultiLineString, personTT:Double, comTT:Double" // ...
			);
			CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
			this.featureType = DataUtilities.createSubType(type, null, crs);
		} catch (SchemaException e) {
			throw new RuntimeException(e);
		}

	}
	
	private Collection<SimpleFeature> createFeatures() {
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (Id personId : routesHandler.getAgentRoutes().keySet()) {
			SimpleFeature feature = getFeature(personId);
			if (feature != null)
				features.add(feature);
		}
		return features;
	}
	
	private SimpleFeature getFeature(Id agentId) {

			LineString[] route = new LineString[routesHandler.getAgentRoute(agentId).size()];
			int i = 0;
			for (Id linkId : routesHandler.getAgentRoute(agentId)) {
				Link link = scenario.getNetwork().getLinks().get(linkId);

				route[i] = this.geometryFactory
						.createLineString(new Coordinate[] {
								MGC.coord2Coordinate(//ct.transform(
										link.getFromNode().getCoord())//),
								,MGC.coord2Coordinate(//ct.transform(
										link.getToNode().getCoord())//)
										});
				i++;
			}

			SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
					featureType);
			// in reihenfolge der spalten attribute reinschreiben
			featureBuilder.add(agentId);
			String comId = routesHandler.getComIdOfAgent(agentId);
			featureBuilder.add(comId);
			featureBuilder.add(this.geometryFactory
					.createMultiLineString(route));
			featureBuilder.add(travelTimesHandler.getPersonTravelTimes().get(agentId));
			featureBuilder.add(travelTimesHandler.getComTravelTimes().get(comId));
			
			SimpleFeature feature = featureBuilder.buildFeature(null);
			return feature;
	}
	
	
	
}
