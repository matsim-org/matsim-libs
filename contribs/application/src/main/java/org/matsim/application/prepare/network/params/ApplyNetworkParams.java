package org.matsim.application.prepare.network.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.matsim.application.prepare.network.params.NetworkParamsOpt.Feature;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.util.Map;
import java.util.Set;

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

	@CommandLine.Parameters(arity = "1..*", description = "Type of parameters to apply. Available: ${COMPLETION-CANDIDATES}")
	private Set<NetworkAttribute> params;

	@CommandLine.Option(names = "--input-params", description = "Path to parameter json")
	private String inputParams;

	@CommandLine.Option(names = "--model", description = "Reference to the network model class", required = true)
	private Class<? extends NetworkModel> modelClazz;

	@CommandLine.Option(names = "--factor-bounds", split = ",", description = "Speed factor limits (lower,upper bound)", defaultValue = NetworkParamsOpt.DEFAULT_FACTOR_BOUNDS)
	private double[] speedFactorBounds;

	private NetworkModel model;
	private NetworkParams paramsOpt;

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
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

		if (inputParams != null) {
			try (BufferedReader in = IOUtils.getBufferedReader(inputParams)) {
				paramsOpt = mapper.readValue(in, NetworkParams.class);
			}
		}

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

		boolean modified = false;

		if (params.contains(NetworkAttribute.capacity)) {

			FeatureRegressor capacity = model.capacity(junctionType);

			double perLane = capacity.predict(features);

			double cap = capacityEstimate(features.getDouble("speed"));

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
		}


		if (params.contains(NetworkAttribute.freespeed)) {

			double speedFactor = 1.0;
			FeatureRegressor speedModel = model.speedFactor(junctionType);

			speedFactor =  paramsOpt != null ?
				speedModel.predict(features, paramsOpt.getParams(junctionType)) :
				speedModel.predict(features);

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

			link.setFreespeed((double) link.getAttributes().getAttribute("allowed_speed") * speedFactor);
			link.getAttributes().putAttribute("speed_factor", speedFactor);
		}

		if (modified)
			warn++;
	}
}
