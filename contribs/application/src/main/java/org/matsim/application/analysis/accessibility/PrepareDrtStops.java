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
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import picocli.CommandLine;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.matsim.core.scenario.ScenarioUtils.createScenario;

@CommandLine.Command(
	name = "prepare-pois", description = "Prepare POIs.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"stops.shp"
	}
)




public class PrepareDrtStops implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(PrepareDrtStops.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PrepareDrtStops.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PrepareDrtStops.class);
	public SimpleFeatureBuilder builder;


	public static void main(String[] args) {
		new PrepareDrtStops().execute(args);
	}

	@Override
	public Integer call() throws Exception {


		// set up coordinate reference systems and transformations
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:25832", true);

		// set up shape file builder
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setCRS(sourceCRS); // this needs to happen before setting "the_geom" for Point.class
		typeBuilder.setName("stop");
		typeBuilder.add("the_geom", Point.class); // this geometry field must be named "the_geom" for GeoTools to recognize it
		typeBuilder.add("ID",String.class);
		typeBuilder.add("type",String.class);
		builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());

		String inputPath = input.getRunDirectory() + "/drt-stops-stadt-und-land.xml";
		String outputPath = input.getRunDirectory() + "/analysis/accessibility/stops.shp";

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


		Scenario scenario = createScenario(ConfigUtils.createConfig());
		TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenario);
		transitScheduleReader.readFile(inputPath);

		List<SimpleFeature> features = new ArrayList<>();
		for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()) {
			Point p = MGC.coord2Point(stop.getCoord());

			features.add(builder.buildFeature(null, p, stop.getId().toString(), "stop"));

		}

		GeoFileWriter.writeGeometries(features, outputPath);


		return 0;
	}


}
