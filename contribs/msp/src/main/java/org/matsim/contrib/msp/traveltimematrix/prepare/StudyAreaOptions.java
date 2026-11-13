package org.matsim.contrib.msp.traveltimematrix.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.util.Set;

/**
 * The three options every command of this package needs to answer "which network, and which part of
 * it": {@code --network}, {@code --shp} and {@code --modes}.
 * <p>
 * A {@link CommandLine.Mixin} rather than three options repeated four times, because the study area has
 * to be cut <b>identically</b> everywhere or the results cannot be compared. The congestion index, the
 * matrix, the evaluation and an experiment all have to see the same nodes: a link id that one of them
 * dropped and another kept is a travel time measured over a different network. Having one place that
 * reads the network is what makes that true by construction rather than by everyone remembering to pass
 * the same flags.
 *
 * @see StudyAreaNetwork what the cutting actually does, and why a cleaned network can still be
 * unroutable
 */
public final class StudyAreaOptions {

	private static final Logger log = LogManager.getLogger(StudyAreaOptions.class);

	@CommandLine.Option(names = "--network", description = "Network of the run. May be larger than the study "
		+ "area - it is cut down to --shp.", required = true)
	private String networkFile;

	@CommandLine.Option(names = "--modes", description = "Modes of the subnetwork. Link ids are kept, so the "
		+ "travel times and volumes of the full run still apply to it.", defaultValue = "car", split = ",")
	private Set<String> modes;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	/**
	 * Reads the network and cuts the study area out of it.
	 *
	 * @return the cleaned, routable subnetwork of the modes, over the shape if one was given and over the
	 * whole network otherwise
	 */
	public Network readSubnetwork() {
		Network fullNetwork = readFullNetwork();
		Network network = StudyAreaNetwork.cut(fullNetwork, StudyArea.fromShapeFile(this.shp, fullNetwork),
			this.modes);
		log.info("Study area: {} links and {} nodes of {} modes, cut from {}.", network.getLinks().size(),
			network.getNodes().size(), this.modes, this.networkFile);
		return network;
	}

	/**
	 * The network of {@code --network}, <b>uncut</b>.
	 * <p>
	 * For the one caller that cuts more than once:
	 * {@link org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixCostExperiment} cuts the same
	 * network to a dozen radii, and a Germany-wide network file is tens of megabytes of gzipped xml to
	 * parse - so it is read once here and cut repeatedly, rather than read once per radius.
	 */
	public Network readFullNetwork() {
		return NetworkUtils.readNetwork(this.networkFile);
	}

	/**
	 * @return the modes the subnetwork is cut for, which is also what a travel time has to be measured
	 * for
	 */
	public Set<String> getModes() {
		return this.modes;
	}

	public String getNetworkFile() {
		return this.networkFile;
	}

	/**
	 * @return the {@code --shp} options, for the one caller that needs more of the shape file than "is
	 * this coordinate inside it":
	 * {@link org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixCostExperiment} takes its
	 * centroid as the centre of a radius sweep.
	 */
	public ShpOptions getShp() {
		return this.shp;
	}
}
