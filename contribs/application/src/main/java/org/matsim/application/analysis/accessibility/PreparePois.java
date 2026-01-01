package org.matsim.application.analysis.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.utils.gis.GeoFileWriter;
import picocli.CommandLine;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "prepare-pois", description = "Prepare POIs.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"%s/pois.shp"
	}
)




public class PreparePois implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(PreparePois.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PreparePois.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PreparePois.class);
	public SimpleFeatureBuilder builder;

	@CommandLine.Mixin
	private final CrsOptions crs = new CrsOptions();



	public static void main(String[] args) {
		new PreparePois().execute(args);
	}

	@Override
	public Integer call() throws Exception {


		// set up coordinate reference systems and transformations
		CoordinateReferenceSystem sourceCRS = CRS.decode(crs.getInputCRS(), true);

		// set up shape file builder
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setCRS(sourceCRS); // this needs to happen before setting "the_geom" for Point.class
		typeBuilder.setName("poi");
		typeBuilder.add("the_geom", Point.class); // this geometry field must be named "the_geom" for GeoTools to recognize it
		typeBuilder.add("ID",String.class);
		typeBuilder.add("type",String.class);
		builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());


		Set<String> activityOptions = null;
		try {
			activityOptions = Files.list(input.getRunDirectory().resolve("analysis/accessibility/"))
				.filter(Files::isDirectory)
				.map(Path::getFileName)
				.map(Path::toString).
				collect(Collectors.toSet());
		} catch (IOException e) {
			e.printStackTrace();
		}


		for (String activityOption : activityOptions) {
			String outputPath = input.getRunDirectory() + "/analysis/accessibility/" + activityOption + "/pois.shp";
			String inputPath = input.getRunDirectory() + "/analysis/accessibility/" + activityOption + "/pois.csv";


			try {
				Path path = Path.of(outputPath);
				if (Files.exists(path)) {
					Files.delete(path);
					System.out.println("File deleted: " + outputPath);
				} else {
					System.out.println("File does not exist: " + outputPath);
				}
			} catch (IOException e) {
				System.err.println("Failed to delete file: " + e.getMessage());
			}


			try {
				// Use CsvReadOptions to configure the CSV reading options
				CsvReadOptions options = CsvReadOptions.builder(inputPath)
					.separator(',')        // Specify the separator if it's not a comma
					.header(true)          // Set to false if the file does not have a header
					.missingValueIndicator("") // Define how missing values are represented
					.columnTypes(new ColumnType[] {
						ColumnType.DOUBLE,  // column 1
						ColumnType.DOUBLE   // column 2
					})
					.build();

				// Read the CSV file into a Table object
				Table table = Table.read().csv(options);

				Collection<SimpleFeature> features = new ArrayList<>();

				for (int row = 0; row < table.rowCount(); row++) {
					GeometryFactory geometryFactory = new GeometryFactory();
					Point p = geometryFactory.createPoint(new Coordinate(table.doubleColumn("xcoord").get(row), table.doubleColumn("ycoord").get(row)));

					features.add(builder.buildFeature(null, p, String.valueOf(row), activityOption));

				}

				GeoFileWriter.writeGeometries(features, outputPath);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return 0;
	}


}
