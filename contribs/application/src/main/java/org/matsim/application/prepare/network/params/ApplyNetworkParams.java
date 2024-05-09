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
import org.matsim.application.prepare.Predictor;
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

	@CommandLine.Option(names = "--capacity-bounds", split = ",", description = "Relative capacity bounds against theoretical max (lower,upper bound)", defaultValue = "0.3,1.0")
	private double[] capacityBounds;

	@CommandLine.Option(names = "--road-types", split = ",", description = "Road types to apply changes to")
	private Set<String> roadTypes;

	@CommandLine.Option(names = "--junction-types", split = ",", description = "Junction types to apply changes to")
	private Set<String> junctionTypes;

	private NetworkModel model;
	private NetworkParams paramsOpt;

	private int warn = 0;

	public static void main(String[] args) {
		new ApplyNetworkParams().execute(args);
	}

	/**
	 * Theoretical capacity.
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
			}  catch (IllegalArgumentException e) {
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

			Predictor capacity = model.capacity(ft.junctionType(), ft.highwayType());
			// No operation performed if not supported
			if (capacity == null) {
				return;
			}

			double perLane = capacity.predict(ft.features(), ft.categories());

			double cap = capacityEstimate(ft.features().getDouble("speed"));

			if (perLane < cap * capacityBounds[0]) {
				log.warn("Increasing capacity per lane on {} ({}, {}) from {} to {}",
					link.getId(), ft.highwayType(), ft.junctionType(), perLane, cap * capacityBounds[0]);
				perLane = cap * capacityBounds[0];
				modified = true;
			}

			if (perLane > cap * capacityBounds[1]) {
				log.warn("Reducing capacity per lane on {} ({}, {}) from {} to {}",
					link.getId(), ft.highwayType(), ft.junctionType(), perLane, cap * capacityBounds[1]);
				perLane = cap * capacityBounds[1];
				modified = true;
			}

			link.setCapacity(link.getNumberOfLanes() * perLane);
		}


		if (params.contains(NetworkAttribute.freespeed)) {

			Predictor speedModel = model.speedFactor(ft.junctionType(), ft.highwayType());

			// No operation performed if not supported
			if (speedModel == null) {
				return;
			}

			double speedFactor =  paramsOpt != null ?
				speedModel.predict(ft.features(), ft.categories(), paramsOpt.getParams(ft.junctionType())) :
				speedModel.predict(ft.features(), ft.categories());

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
