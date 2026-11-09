package org.matsim.application.analysis.population;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TripsAndLegsCsvWriterTest {

	/**
	 * Leg columns that are derived from the vehicle id of a leg, which is not stored in the experienced plans file.
	 */
	private static final Set<String> VEHICLE_COLUMNS = Set.of("vehicle_id", "network_mode");

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@ParameterizedTest
	@ValueSource(strings = {"equil", "pt-simple"})
	void writesSameFilesAsSimulation(String scenarioName) throws IOException {
		Path runDirectory = Path.of(utils.getOutputDirectory(), scenarioName, "run");
		Path analysisDirectory = Path.of(utils.getOutputDirectory(), scenarioName, "analysis");

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL(scenarioName), "config.xml"));
		config.controller().setOutputDirectory(runDirectory.toString());
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		new Controler(config).run();

		// the run writes zst files, so this also checks that given output paths are used as they are
		Path trips = analysisDirectory.resolve("trips.csv.gz");
		Path legs = analysisDirectory.resolve("legs.csv.gz");

		new TripsAndLegsCsvWriter().execute(
			"--run-directory", runDirectory.toString(),
			"--output-trips", trips.toString(),
			"--output-legs", legs.toString()
		);

		List<String> expectedTrips = readLines(ApplicationUtils.matchInput("trips.csv", runDirectory));
		assertThat(expectedTrips).hasSizeGreaterThan(1);
		assertSameHeaderAndRows(readLines(trips), expectedTrips);

		List<List<String>> expectedLegs = readLegsWithoutVehicleColumns(ApplicationUtils.matchInput("legs.csv", runDirectory));
		assertThat(expectedLegs).hasSizeGreaterThan(1);
		assertSameHeaderAndRows(readLegsWithoutVehicleColumns(legs), expectedLegs);
	}

	@Test
	void detectsCompressionExtension() {
		assertThat(TripsAndLegsCsvWriter.compressionExtension(Path.of("run", "test.output_experienced_plans.xml.zst"))).isEqualTo(".zst");
		assertThat(TripsAndLegsCsvWriter.compressionExtension(Path.of("output_experienced_plans.xml.gz"))).isEqualTo(".gz");
		assertThat(TripsAndLegsCsvWriter.compressionExtension(Path.of("output_experienced_plans.xml"))).isEmpty();
	}

	@Test
	void appendsCompressionOnlyToDefaultOutputPaths() {
		Path givenTrips = Path.of(utils.getOutputDirectory(), "my_trips.csv");

		TripsAndLegsCsvWriter command = (TripsAndLegsCsvWriter) new TripsAndLegsCsvWriter()
			.withArgs("--run-directory", utils.getOutputDirectory(), "--output-trips", givenTrips.toString());

		assertThat(command.outputPath("trips.csv", "--output-trips", ".zst")).isEqualTo(givenTrips);
		assertThat(command.outputPath("legs.csv", "--output-legs", ".zst")).isEqualTo(Path.of("legs.csv.zst"));
		assertThat(command.outputPath("legs.csv", "--output-legs", "")).isEqualTo(Path.of("legs.csv"));
	}

	/**
	 * The experienced plans file stores the persons sorted by id, while the simulation writes them in the order of its
	 * internal map. Hence, only the header has to be at the same position.
	 */
	private static <T> void assertSameHeaderAndRows(List<T> actual, List<T> expected) {
		assertThat(actual).first().isEqualTo(expected.getFirst());
		assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
	}

	private static List<String> readLines(Path path) throws IOException {
		try (BufferedReader reader = IOUtils.getBufferedReader(path.toString())) {
			return reader.lines().toList();
		}
	}

	/**
	 * Returns the header and all records of a legs file, without the columns in {@link #VEHICLE_COLUMNS}.
	 */
	private static List<List<String>> readLegsWithoutVehicleColumns(Path path) throws IOException {
		CSVFormat format = CSVFormat.Builder.create().setDelimiter(';').setHeader().setSkipHeaderRecord(true).build();
		try (CSVParser parser = format.parse(IOUtils.getBufferedReader(path.toString()))) {
			List<String> columns = parser.getHeaderNames().stream()
				.filter(column -> !VEHICLE_COLUMNS.contains(column))
				.toList();

			List<List<String>> rows = new ArrayList<>();
			rows.add(columns);
			for (CSVRecord record : parser) {
				rows.add(columns.stream().map(record::get).toList());
			}
			return rows;
		}
	}
}
