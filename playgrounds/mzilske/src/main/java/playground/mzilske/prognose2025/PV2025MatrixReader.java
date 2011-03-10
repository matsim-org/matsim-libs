package playground.mzilske.prognose2025;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mzilske.pipeline.PopulationWriterTask;
import playground.mzilske.pipeline.RoutePersonTask;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;




public class PV2025MatrixReader {

	private static final Logger log = Logger.getLogger(PV2025MatrixReader.class);

	private static final String PV_MATRIX = "/Users/michaelzilske/workspace/prognose_2025/orig/pv-matrizen/2004_nuts_102r6x6.csv";

	private static final String NODES = "/Users/michaelzilske/workspace/prognose_2025/orig/netze/netz-2004/strasse/knoten_wgs84.csv";

	private Map<Integer, Zone> zones = new HashMap<Integer, Zone>();

	private static final String FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/naechster_versuch.xml";

	private static final String NETWORK_FILENAME = "/Users/michaelzilske/osm/motorway_germany.xml";

	private static final String FILTER_FILENAME = "/Users/michaelzilske/workspace/prognose_2025/demand/filter.shp";

	private TripFlowSink flowSink;
	
	public void run() {
		readNodes();
		readMatrix();
		flowSink.complete();
	}

	private void readNodes() {
		final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(NODES);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		try {
			new TabularFileParser().parse(tabFileParserConfig,
					new TabularFileHandler() {
				@Override
				public void startRow(String[] row) {
					if (row[0].startsWith("Knoten")) {
						return;
					}
					int zone = Integer.parseInt(row[5]);
					double x = Double.parseDouble(row[2]);
					double y = Double.parseDouble(row[3]);
					Zone zone1 = new Zone(zone, 1, 1, coordinateTransformation.transform(new CoordImpl(x,y)));
					zones.put(zone, zone1);
				}

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void readMatrix() {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(PV_MATRIX);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		try {
			new TabularFileParser().parse(tabFileParserConfig,
					new TabularFileHandler() {

				@Override
				public void startRow(String[] row) {
					if (row[0].startsWith("#")) {
						return;
					}
					int quelle = Integer.parseInt(row[0]);
					int ziel = Integer.parseInt(row[1]);
					int workPt = Integer.parseInt(row[2]);
					int educationPt = Integer.parseInt(row[3]);
					int workCar = Integer.parseInt(row[8]);
					int educationCar = Integer.parseInt(row[9]);
					process(quelle, ziel, workPt, educationPt, workCar, educationCar);
				}

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isCoordInShape(Coord linkCoord, Set<Feature> features, GeometryFactory factory) {
		boolean found = false;
		Geometry geo = factory.createPoint(new Coordinate(linkCoord.getX(), linkCoord.getY()));
		for (Feature ft : features) {
			if (ft.getDefaultGeometry().contains(geo)) {
				found = true;
				break;
			}
		}
		return found;
	}
	
	private void process(int quelle, int ziel, int workPt, int educationPt, int workCar, int educationCar) {
		Zone source = zones.get(quelle);
		Zone sink = zones.get(ziel);
		if (source == null) {
			log.error("Unknown source: " + quelle);
			return;
		}
		if (sink == null) {
			log.error("Unknown sink: " + ziel);
			return;
		}
		int carQuantity = getCarQuantity(source, sink, (workCar + educationCar)) / 255;
		int scaledCarQuantity = scale(carQuantity);
		if (scaledCarQuantity != 0) {
			log.info(quelle + "->" + ziel + ": "+scaledCarQuantity);
			flowSink.process(zones.get(quelle), zones.get(ziel), scaledCarQuantity, TransportMode.car, "work", 0.0);
		}
	}

	public static void main(String[] args) {
		PopulationGenerator populationBuilder = new PopulationGenerator();
		Scenario osmNetwork = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(osmNetwork).readFile(NETWORK_FILENAME);
		RouterFilter routerFilter = new RouterFilter(osmNetwork.getNetwork());
		Set<Feature> featuresInShape;
		try {
			featuresInShape = new ShapeFileReader().readFileAndInitialize(FILTER_FILENAME);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		GeometryFactory factory = new GeometryFactory();
		for (Node node : osmNetwork.getNetwork().getNodes().values()) {
			if (isCoordInShape(node.getCoord(), featuresInShape, factory)) {
				routerFilter.getInterestingNodeIds().add(node.getId());
			}
		}
		PopulationWriterTask populationWriter = new PopulationWriterTask(FILENAME, osmNetwork.getNetwork());
		RoutePersonTask router = new RoutePersonTask(osmNetwork.getConfig(), osmNetwork.getNetwork());
		PV2025MatrixReader pvMatrixReader = new PV2025MatrixReader();
		pvMatrixReader.setFlowSink(routerFilter);
		routerFilter.setSink(populationBuilder);
		populationBuilder.setSink(router);
		router.setSink(populationWriter);
		pvMatrixReader.run();
	}

	private int getCarQuantity(Zone source, Zone sink, int carWorkTripsPerDay) {
		double outWeight = ((double) source.workingPopulation * sink.workplaces) /  ((double) source.workplaces * sink.workingPopulation);
		double inWeight = ((double) source.workplaces * sink.workingPopulation) /  ((double) source.workingPopulation * sink.workplaces);
		double outShare = outWeight / (inWeight + outWeight);
		int amount = (int) (outShare * carWorkTripsPerDay * 0.5);
		return amount;
	}

	private int scale(int quantityOut) {
		int scaled = (int) (quantityOut * 0.01);
		return scaled;
	}

	void setFlowSink(TripFlowSink flowSink) {
		this.flowSink = flowSink;
	}
	
}
