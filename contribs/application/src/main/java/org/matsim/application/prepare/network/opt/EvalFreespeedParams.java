package org.matsim.application.prepare.network.opt;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.traffic.traveltime.SampleValidationRoutes;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.application.prepare.network.opt.NetworkParamsOpt.*;

@CommandLine.Command(
	name = "eval-network-params", description = "Evaluate network params"
)
@CommandSpec(
	requireNetwork = true,
	requires = "features.csv",
	produces = "eval_speed_factors.csv"
)
public class EvalFreespeedParams implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(EvalFreespeedParams.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(EvalFreespeedParams.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(EvalFreespeedParams.class);

	@CommandLine.Parameters(arity = "1..*", description = "Input validation files loaded from APIs")
	private List<String> validationFiles;

	@CommandLine.Option(names = "--model", description = "Reference to the network model class", required = true)
	private Class<? extends NetworkModel> modelClazz;

	@CommandLine.Option(names = "--params", description = "Apply params to model")
	private Path params;

	@CommandLine.Option(names = "--ref-hours", description = "Reference hours", defaultValue = "3,21", split = ",")
	private List<Integer> refHours;

	@CommandLine.Option(names = "--min-speed-factor", description = "Minimum speed factor", defaultValue = DEFAULT_MIN_SPEED)
	private double minSpeedFactor;

	private Network network;
	private NetworkModel model;
	private Object2DoubleMap<SampleValidationRoutes.FromToNodes> validationSet;
	private Map<Id<Link>, Feature> features;

	public static void main(String[] args) {
		new EvalFreespeedParams().execute(args);
	}

	static Result applyAndEvaluateParams(
		Network network, NetworkModel model, Object2DoubleMap<SampleValidationRoutes.FromToNodes> validationSet, Map<Id<Link>, Feature> features,
		double minFactor, Request request, String save) throws IOException {

		Map<Id<Link>, double[]> attributes = new HashMap<>();

		if (request != null) {
			for (Link link : network.getLinks().values()) {

				double allowedSpeed = NetworkUtils.getAllowedSpeed(link);

				if (request.f <= 0) {

					Feature ft = features.get(link.getId());

					if (ft == null || ft.junctionType().equals("dead_end")) {
//						log.warn("Unknown link {}: {}", link.getId(), ft);
						link.setFreespeed(allowedSpeed);
						continue;
					}

					FeatureRegressor speedModel = model.speedFactor(ft.junctionType());

					if (speedModel == null) {
						link.setFreespeed(allowedSpeed);
						continue;
					}

					double speedFactor;

					if (request.hasParams()) {
						double[] p = request.getParams(ft.junctionType());
						speedFactor = Math.max(minFactor, speedModel.predict(ft.features(), p));
					} else
						speedFactor = Math.max(minFactor, speedModel.predict(ft.features()));

					attributes.put(link.getId(), speedModel.getData(ft.features()));

					link.setFreespeed(allowedSpeed * speedFactor);
					link.getAttributes().putAttribute("speed_factor", speedFactor);

				} else
					// Old MATSim freespeed logic
					link.setFreespeed(LinkProperties.calculateSpeedIfSpeedTag(allowedSpeed, request.f));
			}
		}

		Result result = evaluate(network, validationSet, features, attributes, save);

		log.info("{}, rmse: {}, mae: {}", request, result.rmse(), result.mae());

		return result;
	}

	@Override
	public Integer call() throws Exception {

		model = load(modelClazz);
		network = input.getNetwork();
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

		validationSet = readValidation(validationFiles, refHours);
		features = readFeatures(input.getPath("features.csv"), network.getLinks().size());

		log.info("Model score:");
		applyAndEvaluateParams(network, model, validationSet, features, minSpeedFactor, new Request(0), null);

		if (params != null) {
			log.info("Model with parameter score:");
			applyAndEvaluateParams(network, model, validationSet, features, minSpeedFactor,
				mapper.readValue(params.toFile(), Request.class), null);
		}


		log.info("Evaluating free-speed factors");
		evalSpeedFactors(output.getPath());

		return 0;
	}

	private void evalSpeedFactors(Path eval) throws IOException {

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(eval), CSVFormat.DEFAULT)) {
			csv.printRecord("urban_speed_factor", "mae", "rmse");

			for (int i = 25; i <= 100; i ++) {
				Result res = applyAndEvaluateParams(network, model, validationSet, features, 0,
					new Request(i / 100d), null);
				csv.printRecord(i / 100d, res.mae(), res.rmse());
			}

		}
	}


}
