package org.matsim.application.prepare.network.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.prepare.Predictor;
import org.matsim.application.prepare.network.params.NetworkParamsOpt.Feature;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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

	@CommandLine.Option(names = "--factor-bounds", split = ",", description = "Speed factor limits (lower, upper bound). " +
		"Can be negative to indicate absolute speed bounds (in km/h)", defaultValue = NetworkParamsOpt.DEFAULT_FACTOR_BOUNDS)
	private double[] speedFactorBounds;

	@CommandLine.Option(names = "--capacity-bounds", split = ",", defaultValue = "0.4,0.6,0.8",
		description = "Minimum relative capacity against theoretical max (traffic light,right before left, priority)")
	private List<Double> capacityBounds;

	@CommandLine.Option(names = "--road-types", split = ",", description = "Road types to apply changes to")
	private Set<String> roadTypes;

	@CommandLine.Option(names = "--junction-types", split = ",", description = "Junction types to apply changes to")
	private Set<String> junctionTypes;

	@CommandLine.Option(names = "--decrease-only", description = "Only set values if the are lower than the current value", defaultValue = "false")
	private boolean decrease;

	private NetworkModel model;
	private NetworkParams paramsOpt;

	private int warn = 0;

	public static void main(String[] args) {
		new ApplyNetworkParams().execute(args);
	}

	/**
	 * Theoretical capacity assuming fixed car length and headway. This should usually not be exceeded.
	 */
	public static double capacityEstimate(double v) {

		// headway
		double tT = 1.2;

		// car length + buffer
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

		Map<Id<Link>, Feature> features = NetworkParamsOpt.readFeatures(input.getPath("features.csv"), network.getLinks());

		for (Link link : network.getLinks().values()) {
			Feature ft = features.get(link.getId());

			if (roadTypes != null && !roadTypes.isEmpty() && !roadTypes.contains(ft.highwayType()))
				continue;

			if (junctionTypes != null && !junctionTypes.isEmpty() && !junctionTypes.contains(ft.junctionType()))
				continue;

			try {
				applyChanges(link, ft);
			} catch (IllegalArgumentException e) {
				warn++;
				log.warn("Error processing link {}", link.getId(), e);
			}
		}

		log.warn("Observed {} warnings out of {} links", warn, network.getLinks().size());

		NetworkUtils.writeNetwork(network, output.getPath("network.xml.gz").toString());

		return 0;
	}

	/**
	 * Apply speed and capacity models and apply changes.
	 */
	private void applyChanges(Link link, Feature ft) {

		boolean modified = false;

		if (params.contains(NetworkAttribute.capacity)) {
			modified = applyCapacity(link, ft);
		}

		if (params.contains(NetworkAttribute.freespeed)) {
			modified |= applyFreeSpeed(link, ft);
		}

		if (modified)
			warn++;
	}

	private boolean applyCapacity(Link link, Feature ft) {

		Predictor capacity = model.capacity(ft.junctionType(), ft.highwayType());
		// No operation performed if not supported
		if (capacity == null) {
			return false;
		}

		double perLane = capacity.predict(ft.features(), ft.categories());
		if (Double.isNaN(perLane)) {
			return true;
		}

		double cap = capacityEstimate(ft.features().getDouble("speed"));

		if (capacityBounds.isEmpty())
			capacityBounds.add(0.0);

		// Fill up to 3 elements if not provided
		if (capacityBounds.size() < 3) {
			capacityBounds.add(capacityBounds.get(0));
			capacityBounds.add(capacityBounds.get(1));
		}

		// Minimum thresholds
		double threshold = switch (ft.junctionType()) {
			case "traffic_light" -> capacityBounds.get(0);
			case "right_before_left" -> capacityBounds.get(1);
			case "priority" -> capacityBounds.get(2);
			default -> 0;
		};

		boolean modified = false;

		if (perLane < cap * threshold) {
			log.warn("Increasing capacity per lane on {} ({}, {}) from {} to {}",
				link.getId(), ft.highwayType(), ft.junctionType(), perLane, cap * threshold);
			perLane = cap * threshold;
			modified = true;
		}

		if (perLane > cap) {
			log.warn("Reducing capacity per lane on {} ({}, {}) from {} to {}",
				link.getId(), ft.highwayType(), ft.junctionType(), perLane, cap);
			perLane = cap;
			modified = true;
		}

		if (ft.features().getOrDefault("num_lanes", link.getNumberOfLanes()) != link.getNumberOfLanes())
			log.warn("Number of lanes for link {} does not match the feature file", link.getId());

		int totalCap = BigDecimal.valueOf(link.getNumberOfLanes() * perLane).setScale(0, RoundingMode.HALF_UP).intValue();

		if (decrease && totalCap > link.getCapacity())
			return false;

		link.setCapacity(totalCap);

		return modified;
	}

	private boolean applyFreeSpeed(Link link, Feature ft) {

		Predictor speedModel = model.speedFactor(ft.junctionType(), ft.highwayType());

		// No operation performed if not supported
		if (speedModel == null) {
			return false;
		}

		double speedFactor = paramsOpt != null ?
			speedModel.predict(ft.features(), ft.categories(), paramsOpt.getParams(ft.junctionType())) :
			speedModel.predict(ft.features(), ft.categories());

		if (Double.isNaN(speedFactor)) {
			return false;
		}

		double allowedSpeed = (double) link.getAttributes().getAttribute("allowed_speed");
		double freeSpeed = allowedSpeed * speedFactor;

		boolean modified = false;

		if (speedFactor > speedFactorBounds[1] && speedFactorBounds[1] >= 0) {
			log.warn("Reducing speed factor on {} from {} to {}", link.getId(), speedFactor, speedFactorBounds[1]);
			speedFactor = speedFactorBounds[1];
			modified = true;
		}

		// Use absolute bound for speed
		if (freeSpeed > -speedFactorBounds[1]/3.6 && speedFactorBounds[1] < 0) {
			log.warn("Reducing speed on {} from {} to {}", link.getId(), freeSpeed, -speedFactorBounds[1]/3.6);
			speedFactor = (-speedFactorBounds[1] / 3.6) / allowedSpeed;
			modified = true;
		}

		// Threshold for very low speed factors
		if (speedFactor < speedFactorBounds[0] && speedFactorBounds[0] >= 0) {
			log.warn("Increasing speed factor on {} from {} to {}", link, speedFactor, speedFactorBounds[0]);
			speedFactor = speedFactorBounds[0];
			modified = true;
		}

		// Absolute negative speed factor
		if (freeSpeed < -speedFactorBounds[0]/3.6 && speedFactorBounds[0] < 0) {
			log.warn("Increasing speed on {} from {} to {}", link, freeSpeed, -speedFactorBounds[0]/3.6);
			speedFactor = (-speedFactorBounds[0] / 3.6) / allowedSpeed;
			modified = true;
		}

		// Recalculate with updated speed factor
		freeSpeed = allowedSpeed * speedFactor;
		freeSpeed = BigDecimal.valueOf(freeSpeed).setScale(3, RoundingMode.HALF_EVEN).doubleValue();

		if (decrease && freeSpeed > link.getFreespeed())
			return false;

		link.setFreespeed(freeSpeed);
		link.getAttributes().putAttribute(
			"speed_factor",
			BigDecimal.valueOf(freeSpeed).setScale(5, RoundingMode.HALF_EVEN).doubleValue()
		);

		return modified;
	}

}
