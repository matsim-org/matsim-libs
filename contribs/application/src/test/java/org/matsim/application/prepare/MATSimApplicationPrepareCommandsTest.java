package org.matsim.application.prepare;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.application.prepare.longDistanceFreightGER.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.population.GenerateShortDistanceTrips;
import org.matsim.application.prepare.population.MergePopulations;
import org.matsim.application.prepare.population.TrajectoryToPlans;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests contrib prepare commands registered through {@link MATSimApplication}.
 */
public class MATSimApplicationPrepareCommandsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void population() throws MalformedURLException {

		Path input = Path.of(utils.getClassInputDirectory());
		Path output = Path.of(utils.getOutputDirectory());

		assertThat(input.resolve("persons.xml")).exists();

		MATSimApplication.execute(ContribTestScenario.class, "prepare", "trajectory-to-plans",
			"--samples", "0.5", "0.1",
			"--sample-size", "1.0",
			"--name", "test",
			"--population", input.resolve("persons.xml").toString(),
			"--attributes", input.resolve("attributes.xml").toString(),
			"--output", output.toString(),
			"--max-typical-duration", "86400" // means that it will run the "split" part.  Which, however, is deprecated.
			// The test should probably be adopted to no longer use those deprecated methods.  kai, nov'26
		);

		Path plans = output.resolve("test-100pct.plans.xml.gz");

		assertThat(plans).exists();
		assertThat(output.resolve("test-50pct.plans.xml.gz")).exists();

		MATSimApplication.execute(ContribTestScenario.class, "prepare", "generate-short-distance-trips",
			"--population", plans.toString(),
			"--num-trips", "2"
		);

		var actualContent = IOUtils.getInputStream(
			output.resolve("test-100pct.plans-with-trips.xml.gz").toUri().toURL());
		var expectedContent = IOUtils.getInputStream(
			input.resolve("test-100pct.plans-with-trips.xml.gz").toUri().toURL());

		assertThat(IOUtils.isEqual(actualContent, expectedContent)).isTrue();
	}

	@Test
	@Disabled("Class is deprecated")
	void freight() {

		Path input = Path.of("..", "..", "..", "..",
			"shared-svn", "komodnext", "data", "freight", "original_data").toAbsolutePath().normalize();

		Assumptions.assumeTrue(Files.exists(input));

		Path output = Path.of(utils.getOutputDirectory());

		String network = input.resolve("german-primary-road.network.xml.gz").toString();

		String allFreightTrips = output.resolve("german-wide-freight-trips.xml.gz").toString();
		MATSimApplication.execute(ContribTestScenario.class, "prepare", "generate-german-freight-trips",
			input.toString(),
			"--sample", "0.25",
			"--network", network,
			"--input-crs", "EPSG:5677",
			"--output", allFreightTrips
		);

		String freightTrips = output.resolve("freight-trips.xml.gz").toString();
		MATSimApplication.execute(ContribTestScenario.class, "prepare", "extract-freight-trips",
			allFreightTrips,
			"--network", network,
			"--shp", input.resolve("../DusseldorfBoundary/newDusseldorfBoundary.shp").toString(),
			"--input-crs", "EPSG:5677",
			"--target-crs", "EPSG:25832",
			"--output", freightTrips
		);
	}

	/**
	 * Scenario fixture registering contrib prepare commands.
	 */
	@MATSimApplication.Prepare({
		TrajectoryToPlans.class, GenerateShortDistanceTrips.class, ExtractRelevantFreightTrips.class, MergePopulations.class
	})
	public static final class ContribTestScenario extends MATSimApplication {
	}
}
