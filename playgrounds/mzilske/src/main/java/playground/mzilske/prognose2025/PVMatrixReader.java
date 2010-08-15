package playground.mzilske.prognose2025;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;



public class PVMatrixReader {

	private static final String PV_MATRIX = "../../prognose_2025/orig/pv-matrizen/2004_nuts_102r6x6.csv";

	private static final String LANDKREISE = "../../prognose_2025/osm_zellen/landkreise.shp";
	
	private static final String NETZ = "../../detailedEval/Net/Analyse2005_Netz.shp";

	private final Set<Integer> seenCellsInMatrix = new HashSet<Integer>();

	private final Set<Integer> seenCellsInShape = new HashSet<Integer>();

	private PopulationGenerator populationBuilder = new PopulationGenerator();

	private Geometry filterShape;

	private static final String FILENAME = "../../detailedEval/pop/hintergrund/plans.xml";
	
	public void run() {
		readFilterShape();
		readShape();
		readMatrix();
		int notFound = 0;
		for (Integer cellid : seenCellsInMatrix) {
			if (! seenCellsInShape.contains(cellid)) {
				++notFound;
			}
		}
		System.out.println("Not found: " + notFound + " of " + seenCellsInMatrix.size());
		int notRequired = 0;
		for (Integer cellid : seenCellsInShape) {
			if (! seenCellsInMatrix.contains(cellid)) {
				++notRequired;
			}
		}
		System.out.println("Not required: " + notRequired + " of " + seenCellsInShape.size());
		System.out.println("Creating " + populationBuilder.countPersons() + " people.");
		populationBuilder.run();
		populationBuilder.writePopulation(FILENAME);
	}

	private void readFilterShape() {
		FeatureSource netz;
		try {
			netz = ShapeFileReader.readDataFile(NETZ);
			filterShape = new GeometryFactory().toGeometry(netz.getFeatures().getBounds());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void readShape() {
		FeatureSource landkreisSource;
		try {
			landkreisSource = ShapeFileReader.readDataFile(LANDKREISE);
			landkreisSource.getFeatures();
			Collection<Feature> landkreise = landkreisSource.getFeatures();
			for (Feature landkreis : landkreise) {
				Integer gemeindeschluessel = Integer.parseInt((String) landkreis.getAttribute("gemeindesc"));
				seenCellsInShape.add(gemeindeschluessel);
				populationBuilder.addZone(gemeindeschluessel, 1, 1, landkreis.getDefaultGeometry());
			}
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
					if (quelle <= 16109 && ziel <= 16109) {
						seenCellsInMatrix.add(quelle);
						seenCellsInMatrix.add(ziel);
						if (seenCellsInShape.contains(quelle) && seenCellsInShape.contains(ziel)) {
							if (populationBuilder.getZones().get(quelle).geometry.getEnvelope().union(populationBuilder.getZones().get(ziel).geometry.getEnvelope()).intersects(filterShape)) {
								populationBuilder.addEntry(quelle, ziel, Integer.parseInt(row[10]), Integer.parseInt(row[4]));
							}
						}
					}
				}

			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		PVMatrixReader pvMatrixReader = new PVMatrixReader();
		pvMatrixReader.run();
	}

}
