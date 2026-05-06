package ch.sbb.matsim.contrib.railsim.prepare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.network.NetworkUtils;

import picocli.CommandLine;

@CommandLine.Command(
	name = "prepare-turning-angle-based-restrictions",
	description = "Set turn restrictions based on the turning angle between two links."
)
public class PrepareTurningAngleBasedRestrictions implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(PrepareTurningAngleBasedRestrictions.class);

	@CommandLine.Option(names = {"--input", "--network"}, required = true, description = "Input network file")
	private String input;

	@CommandLine.Option(names = "--output", required = true, description = "Output network file")
	private String output;

	@CommandLine.Option(names = "--mode", required = true, description = "Network mode of the turn restrictions")
	private String mode;

	@CommandLine.Option(names = "--angle", description = "Maximum turning angle in degrees")
	private double maxTurningAngle = 45;

	@SuppressWarnings("unused")
	public PrepareTurningAngleBasedRestrictions() {
	}

	public PrepareTurningAngleBasedRestrictions(String input, String output, String mode, double maxTurningAngle) {
		this.input = input;
		this.output = output;
		this.mode = mode;
		this.maxTurningAngle = maxTurningAngle;
	}
	PrepareTurningAngleBasedRestrictions(String mode, double maxTurningAngle) {
		this.mode = mode;
		this.maxTurningAngle = maxTurningAngle;
	}

	/**
	 * Set turn restrictions based on the turning angle between two links.
	 */
	public static void setTurnRestrictions(Network network, String mode, double maxTurningAngle) {
		PrepareTurningAngleBasedRestrictions f = new PrepareTurningAngleBasedRestrictions(mode, maxTurningAngle);
		network.getLinks().forEach(f::computeTurnRestrictions);
	}

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(input);

		network.getLinks().forEach(this::computeTurnRestrictions);

		NetworkUtils.writeNetwork(network, output);

		return 0;
	}

	private void computeTurnRestrictions(Id<Link> linkId, Link link) {
		// Skip if the link doesn't allow the specified mode
		if (!link.getAllowedModes().contains(mode)) {
			return;
		}

		// Get all outgoing links from the current link's toNode
		Map<Id<Link>, ? extends Link> outLinks = link.getToNode().getOutLinks();

		if (outLinks.isEmpty()) {
			return;
		}

		// Calculate the vector of the current link
		Coord inLinkVector = getVector(link);

		List<Id<Link>> disallowedLinks = new ArrayList<>();

		// Check each outgoing link
		for (Link outLink : outLinks.values()) {
			// Skip if the outgoing link doesn't allow the specified mode
			if (!outLink.getAllowedModes().contains(mode)) {
				continue;
			}

			// Skip U-turns (links that go back to the fromNode)
			if (outLink.getToNode().equals(link.getFromNode())) {
				continue;
			}

			// Calculate the vector of the outgoing link
			Coord outLinkVector = getVector(outLink);

			// Calculate the turning angle between the two links
			double turningAngle = calculateTurningAngle(inLinkVector, outLinkVector);

			// Convert to degrees for comparison
			double turningAngleDegrees = Math.toDegrees(turningAngle);

			// If the turning angle is greater than the maximum threshold, disallow this turn
			// This means sharp turns (large angles) are restricted, while gentle turns (small angles) are allowed
			if (turningAngleDegrees > maxTurningAngle) {
				disallowedLinks.add(outLink.getId());
				log.debug("Disallowing turn from {} to {} with angle {}° (threshold: {}°)",
					link.getId(), outLink.getId(), turningAngleDegrees, maxTurningAngle);
			}
		}

		// Set the turn restrictions if any disallowed links were found
		if (!disallowedLinks.isEmpty()) {
			NetworkUtils.addDisallowedNextLinks(link, mode, disallowedLinks);
			log.info("Set {} turn restrictions for link {} (mode: {})", disallowedLinks.size(), link.getId(), mode);
		}
	}

	/**
	 * Calculate the vector from a link's fromNode to toNode.
	 */
	Coord getVector(Link link) {
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
		return new Coord(x, y);
	}

	/**
	 * Calculate the turning angle between two vectors in radians.
	 * The angle is always positive and represents the smallest angle between the vectors.
	 */
	double calculateTurningAngle(Coord vector1, Coord vector2) {
		// Calculate the dot product
		double dotProduct = vector1.getX() * vector2.getX() + vector1.getY() * vector2.getY();

		// Calculate the magnitudes
		double magnitude1 = Math.sqrt(vector1.getX() * vector1.getX() + vector1.getY() * vector1.getY());
		double magnitude2 = Math.sqrt(vector2.getX() * vector2.getX() + vector2.getY() * vector2.getY());

		// Avoid division by zero
		if (magnitude1 == 0 || magnitude2 == 0) {
			return 0;
		}

		// Calculate the cosine of the angle
		double cosAngle = dotProduct / (magnitude1 * magnitude2);

		// Clamp the value to [-1, 1] to avoid numerical errors
		cosAngle = Math.max(-1.0, Math.min(1.0, cosAngle));

		// Calculate the angle in radians
		double angle = Math.acos(cosAngle);

		// For turn restrictions, we want the angle that a vehicle would need to turn
		// We return the smaller angle between the two possible directions
		return Math.min(angle, Math.PI - angle);
	}

}
