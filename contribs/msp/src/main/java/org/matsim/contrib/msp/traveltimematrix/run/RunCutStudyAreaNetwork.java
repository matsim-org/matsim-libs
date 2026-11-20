package org.matsim.contrib.msp.traveltimematrix.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyArea;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork;

/**
 * Cuts the study area out of a run's network and <b>writes it to a file</b>, once, so that everything
 * afterwards runs over the small network instead of cutting the large one again:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.prepare.RunCutStudyAreaNetwork \
 *       --network 012.output_network.xml.gz --shp dresden-with-10km-buffer.shp.zip \
 *       --radii 5000,10000,20000 --output-directory ./networks
 * </pre>
 * <p>
 * <b>Why this is worth a command of its own, and not just a convenience.</b> A matrix's sparse half keeps
 * a {@link java.util.BitSet} per row indexed by the <em>global</em> node id index, and MATSim hands those
 * indices out as ids are created and never compacts them. Cutting 22,948 Dresden nodes out of a
 * Germany-wide network in the same JVM therefore leaves them spread over an index space of 530,136, and
 * every row's bit set is sized by that: 22,948 x 530,136/8 = <b>1.5 GB</b> of bit sets, against the 66 MB
 * the study area alone would need. On the resolution grid that one term is larger than everything the
 * grid is sweeping - it appears identically in every configuration and hides the differences between
 * them, so a memory column measured that way describes the Germany-wide network's id space rather than
 * the matrix.
 * <p>
 * Reading a <b>written-out</b> cut network in a fresh JVM creates exactly as many node ids as that network
 * has nodes, which is what collapses the index space. It cannot be done within one JVM: re-reading a
 * network creates new ids <em>next to</em> the old ones and makes the space larger, not smaller.
 * <p>
 * <b>What this does not change</b> is the link ids, which are kept by {@link StudyAreaNetwork#cut} - so a
 * congested travel time or a volume measured on the full run still applies to the cut network, and the
 * congestion index may be measured on either.
 *
 * @see StudyAreaNetwork what the cutting actually does
 * @see org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixCostExperiment the sweep this is the
 * first step of
 */
@CommandLine.Command(
	name = "cut-study-area-network",
	description = "Cuts the study area (a shape, or discs of given radii) out of a network and writes the result, "
		+ "so that later runs see a node id space the size of the study area.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunCutStudyAreaNetwork implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunCutStudyAreaNetwork.class);

	@CommandLine.Option(names = "--output-directory", description = "Directory the networks are written to.",
		defaultValue = "networks")
	private String outputDirectory;

	@CommandLine.Option(names = "--prefix", description = "First part of every file name written.",
		defaultValue = "study-area")
	private String prefix;

	@CommandLine.Option(names = "--radii", description = "Also write one network per radius [m], as discs around "
		+ "--centre. The file of a radius r is '<prefix>_r<r>.xml.gz'.", split = ",")
	private List<Double> radii;

	@CommandLine.Option(names = "--centre", description = "Centre of the discs as 'x,y' in the coordinate system "
		+ "of the network. Defaults to the centroid of --shp, which is logged so that a sweep can be given the "
		+ "same number explicitly.")
	private String centre;

	@CommandLine.Option(names = "--skip-shape-area", description = "Do not write the --shp area itself, only the "
		+ "radii. Nothing is written for the shape when it is not given either way.")
	private boolean skipShapeArea;

	@CommandLine.Mixin
	private StudyAreaOptions studyArea = new StudyAreaOptions();

	public static void main(String[] args) {
		new RunCutStudyAreaNetwork().execute(args);
	}

	@Override
	public Integer call() {

		Path directory = Path.of(this.outputDirectory);
		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create the output directory " + directory, e);
		}

		// read once and cut repeatedly: a Germany-wide network file is tens of megabytes of gzipped xml,
		// and this command exists to be run once over all the areas a study needs
		Network full = this.studyArea.readFullNetwork();
		log.info("Read {} links and {} nodes from {}; this JVM has created {} node ids.",
			full.getLinks().size(), full.getNodes().size(), this.studyArea.getNetworkFile(),
			Id.getNumberOfIds(Node.class));

		List<Path> written = new ArrayList<>();

		if (!this.skipShapeArea) {
			StudyArea area = StudyArea.fromShapeFile(this.studyArea.getShp(), full);
			log.info("--- the {} area ---", area == null ? "whole network" : area);
			written.add(write(StudyAreaNetwork.cut(full, area, this.studyArea.getModes()),
				directory.resolve(this.prefix + "_area.xml.gz")));
		}

		if (this.radii != null && !this.radii.isEmpty()) {
			Coord centreOfTheDiscs = centre(full);
			log.info("Cutting {} disc(s) around {}, {}. Pass '--centre {},{}' to any sweep over these files, so "
					+ "that it labels its rows with the radius it was cut with.", this.radii.size(),
				(long) centreOfTheDiscs.getX(), (long) centreOfTheDiscs.getY(),
				(long) centreOfTheDiscs.getX(), (long) centreOfTheDiscs.getY());

			this.radii.stream().sorted().forEach(radius -> {
				log.info("--- a radius of {} m ---", (long) radius.doubleValue());
				Network cut = StudyAreaNetwork.cut(full, StudyArea.circle(centreOfTheDiscs, radius),
					this.studyArea.getModes());
				written.add(write(cut, directory.resolve(String.format(Locale.ROOT, "%s_r%d.xml.gz", this.prefix,
					(long) radius.doubleValue()))));
			});
		}

		if (written.isEmpty()) {
			throw new IllegalArgumentException("Nothing to write: --skip-shape-area was given and --radii was not.");
		}

		log.info("Wrote {} network(s). Each of them read in a fresh JVM gives a node id space the size of that "
			+ "network, which is the point - see the class javadoc.", written.size());
		written.forEach(file -> log.info("  {}", file));
		return 0;
	}

	/**
	 * Writes one network, with the number of nodes in the file logged next to what the bit sets of a
	 * matrix over it will cost - which is the number this command exists to reduce.
	 */
	private Path write(Network network, Path file) {

		NetworkUtils.writeNetwork(network, file.toString());

		int nodes = network.getNodes().size();
		log.info("Wrote {} links and {} nodes to {}. A sparse matrix over it will spend {} MB on bit sets in a "
				+ "fresh JVM, against {} MB if it were cut out of {} in the JVM that read it.",
			network.getLinks().size(), nodes, file,
			(long) nodes * ((nodes + 7) / 8) / 1024 / 1024,
			(long) nodes * ((Id.getNumberOfIds(Node.class) + 7) / 8) / 1024 / 1024,
			this.studyArea.getNetworkFile());
		return file;
	}

	/**
	 * The centre of the discs: what {@code --centre} says, else the centroid of {@code --shp}, else the
	 * middle of the network's bounding box - the same order
	 * {@link org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixCostExperiment} uses, so that
	 * cutting a disc here and labelling it there cannot disagree about where the city is.
	 */
	private Coord centre(Network full) {

		if (this.centre != null) {
			String[] parts = this.centre.split(",");
			if (parts.length != 2) {
				throw new IllegalArgumentException("--centre has to be 'x,y' in the coordinate system of the "
					+ "network, not '" + this.centre + "'.");
			}
			return new Coord(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
		}

		if (this.studyArea.getShp().isDefined()) {
			return StudyArea.centreOf(this.studyArea.getShp(), full);
		}

		log.warn("No --centre and no --shp, so the discs are cut around the middle of the network's bounding "
			+ "box. That is rarely the city the study is about.");
		return StudyArea.centreOf(full);
	}
}
