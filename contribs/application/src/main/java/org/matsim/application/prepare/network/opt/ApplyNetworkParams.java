package org.matsim.application.prepare.network.opt;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.prepare.network.opt.NetworkParamsOpt.Feature;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.util.Map;

@CommandLine.Command(
	name = "apply-network-params", description = "Apply network parameters for capacity and speed."
)
@CommandSpec(
	requireNetwork = true,
	requires = "features.csv",
	produces = "network.xml.gz"
)
public class ApplyNetworkParams implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(ApplyNetworkParams.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(ApplyNetworkParams.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(ApplyNetworkParams.class);

	@CommandLine.Option(names = "--input-params", description = "Path to parameter json")
	private String inputParams;

	@CommandLine.Option(names = "--model", description = "Reference to the network model class", required = true)
	private Class<? extends NetworkModel> modelClazz;

	@CommandLine.Option(names = "--factor-bounds", split = ",", description = "Speed factor limits (lower,upper bound)", defaultValue = NetworkParamsOpt.DEFAULT_FACTOR_BOUNDS)
	private double[] speedFactorBounds;

	private NetworkModel model;


	private int warn = 0;

	public static void main(String[] args) {
		new ApplyNetworkParams().execute(args);
	}

	/**
	 * Theoretical capacity.
	 */
	private static double capacityEstimate(double v) {

		// headway
		double tT = 1.2;

		// car length
		double lL = 7.0;

		double Qc = v / (v * tT + lL);

		return 3600 * Qc;
	}


	@Override
	public Integer call() throws Exception {

		model = NetworkParamsOpt.load(modelClazz);

		Network network = input.getNetwork();

		// TODO: read parameters, reuse code from free speed optimizer
		// TODO: might need validation files which are not available -> eval should be separete class
		// TODO put eval into this class?

		Map<Id<Link>, Feature> features = NetworkParamsOpt.readFeatures(input.getPath("features.csv"), network.getLinks().size());

		for (Link link : network.getLinks().values()) {
			Feature ft = features.get(link.getId());
			applyChanges(link, ft.junctionType(), ft.features());
		}

		log.warn("Observed {} warnings out of {} links", warn, network.getLinks().size());

		NetworkUtils.writeNetwork(network, output.getPath("network.xml.gz").toString());

		return 0;
	}

	/**
	 * Apply speed and capacity models and apply changes.
	 */
	private void applyChanges(Link link, String junctionType, Object2DoubleMap<String> features) {

		String type = NetworkUtils.getHighwayType(link);

		FeatureRegressor capacity = model.capacity(junctionType);

		double perLane = capacity.predict(features);

		double cap = capacityEstimate(features.getDouble("speed"));

		boolean modified = false;

		// Minimum thresholds
		double threshold = switch (junctionType) {
			// traffic light can reduce capacity at least to 50% (with equal green split)
			case "traffic_light" -> 0.4;
			case "right_before_left" -> 0.6;
			// Motorways are kept at their max theoretical capacity
			case "priority" -> type.startsWith("motorway") ? 1 : 0.8;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};

		if (perLane < cap * threshold) {
			log.warn("Increasing capacity per lane on {} ({}, {}) from {} to {}", link.getId(), type, junctionType, perLane, cap * threshold);
			perLane = cap * threshold;
			modified = true;
		}

		link.setCapacity(link.getNumberOfLanes() * perLane);

		double speedFactor = 1.0;

		if (!type.startsWith("motorway")) {

			FeatureRegressor speedModel = model.speedFactor(junctionType);

			speedFactor = speedModel.predict(features);

			if (speedFactor > speedFactorBounds[1]) {
				log.warn("Reducing speed factor on {} from {} to {}", link.getId(), speedFactor, speedFactorBounds[1]);
				speedFactor = speedFactorBounds[1];
				modified = true;
			}

			// Threshold for very low speed factors
			if (speedFactor < speedFactorBounds[0]) {
				log.warn("Increasing speed factor on {} from {} to {}", link, speedFactor, speedFactorBounds[0]);
				speedFactor = speedFactorBounds[0];
				modified = true;
			}
		}

		if (modified)
			warn++;

		link.setFreespeed((double) link.getAttributes().getAttribute("allowed_speed") * speedFactor);
		link.getAttributes().putAttribute("speed_factor", speedFactor);
	}


}
