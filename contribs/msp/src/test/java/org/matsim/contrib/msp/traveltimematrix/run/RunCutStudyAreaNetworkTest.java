package org.matsim.contrib.msp.traveltimematrix.run;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The point of the command is what a <b>later</b> JVM sees, which no single test can assert - a node id
 * space is global to the JVM and this one has already created ids. So what is checked here is everything
 * the command controls: that it writes the file names a sweep script will look for, that the network in
 * them is the cut one and not the full one, and that the discs grow with the radius.
 */
class RunCutStudyAreaNetworkTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * 10 by 10 nodes 1 km apart, so a 2 km disc around the middle holds a handful of nodes and a 20 km one
	 * holds all of them.
	 */
	private static final int GRID_SIZE = 10;

	private Path networkFile;

	@BeforeEach
	void writeAGridNetwork() {
		this.networkFile = Path.of(utils.getOutputDirectory(), "network.xml");
		NetworkUtils.writeNetwork(TestNetworks.grid(GRID_SIZE), this.networkFile.toString());
	}

	@Test
	void oneNetworkPerRadiusIsWrittenUnderTheNameASweepLooksFor() {

		Path directory = Path.of(utils.getOutputDirectory(), "networks");
		run("--output-directory", directory.toString(), "--prefix", "dresden",
			"--centre", "4500,4500", "--radii", "3000,20000", "--skip-shape-area");

		Path small = directory.resolve("dresden_r3000.xml.gz");
		Path large = directory.resolve("dresden_r20000.xml.gz");
		assertTrue(Files.exists(small), "a file named after the radius it was cut with");
		assertTrue(Files.exists(large));

		Network cutSmall = NetworkUtils.readNetwork(small.toString());
		Network cutLarge = NetworkUtils.readNetwork(large.toString());

		assertTrue(cutSmall.getNodes().size() < cutLarge.getNodes().size(),
			"a 3 km disc holds fewer nodes than a 20 km one");
		assertEquals(GRID_SIZE * GRID_SIZE, cutLarge.getNodes().size(),
			"and a 20 km disc around the middle of a 9 by 9 km grid holds all of it");
	}

	@Test
	void theShapeAreaIsWrittenWhenNoRadiusIsGivenAndTheWholeNetworkWhenThereIsNoShapeEither() {

		Path directory = Path.of(utils.getOutputDirectory(), "area");
		run("--output-directory", directory.toString());

		Path area = directory.resolve("study-area_area.xml.gz");
		assertTrue(Files.exists(area), "the default prefix and the area suffix");
		assertEquals(GRID_SIZE * GRID_SIZE, NetworkUtils.readNetwork(area.toString()).getNodes().size(),
			"with no --shp there is nothing to cut away, only modes to filter by");
	}

	@Test
	void askingForNeitherTheShapeAreaNorAnyRadiusIsAnError() {

		// a command that writes nothing is a script with a typo in it, and failing is how the script's
		// author finds out before the sweep does
		assertThrows(IllegalArgumentException.class, () -> run("--output-directory",
			Path.of(utils.getOutputDirectory(), "nothing").toString(), "--skip-shape-area"));
	}

	private void run(String... options) {

		String[] arguments = new String[options.length + 2];
		arguments[0] = "--network";
		arguments[1] = this.networkFile.toString();
		System.arraycopy(options, 0, arguments, 2, options.length);

		new RunCutStudyAreaNetwork().execute(arguments);
	}
}
