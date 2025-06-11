package org.matsim.application.prepare.network.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
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
import org.matsim.application.prepare.Predictor;
import org.matsim.contrib.osm.networkReader.LinkProperties;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.application.prepare.network.params.NetworkParamsOpt.*;

@CommandLine.Command(
	name = "eval-network-params", description = "Evaluate network params"
)
@CommandSpec(
	requireNetwork = true,
	requires = "features.csv",
	produces = {"eval_speed_factors.csv", "eval.csv"}
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

	@CommandLine.Option(names = "--params-name", description = "Use specified name instead of filename")
	private String paramsName;

	@CommandLine.Option(names = "--ref-hours", description = "Reference hours", defaultValue = "3,21", split = ",")
	private List<Integer> refHours;

	@CommandLine.Option(names = "--factor-bounds", split = ",", description = "Speed factor limits (lower,upper bound)", defaultValue = NetworkParamsOpt.DEFAULT_FACTOR_BOUNDS)
	private double[] speedFactorBounds;

	@CommandLine.Option(names = "--eval-factors", description = "Eval freespeed factors", defaultValue = "false")
	private boolean evalFactors;

	@CommandLine.Option(names = "--eval-detailed", description = "Write detailed csv for each request.")
	private boolean evalDetailed;

	private Network network;
	private NetworkModel model;
	private Object2DoubleMap<SampleValidationRoutes.FromToNodes> validationSet;
	private Map<Id<Link>, Feature> features;

	public static void main(String[] args) {
		new EvalFreespeedParams().execute(args);
	}

	static Result applyAndEvaluateParams(
		Network network, NetworkModel model, Object2DoubleMap<SampleValidationRoutes.FromToNodes> validationSet, Map<Id<Link>, Feature> features,
		double[] speedFactorBounds, NetworkParams request, String save) throws IOException {

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

					Predictor speedModel = model.speedFactor(ft.junctionType(), ft.highwayType());

					if (speedModel == null) {
						link.setFreespeed(allowedSpeed);
						continue;
					}

					double speedFactor;

					if (request.hasParams()) {
						double[] p = request.getParams(ft.junctionType());
						speedFactor = speedModel.predict(ft.features(), ft.categories(), p);
					} else
						speedFactor = speedModel.predict(ft.features(), ft.categories());

					// apply lower and upper bound
					if (speedFactorBounds[0] >= 0)
						speedFactor = Math.max(speedFactorBounds[0], speedFactor);

					if (speedFactorBounds[1] >= 0)
						speedFactor = Math.min(speedFactorBounds[1], speedFactor);

					attributes.put(link.getId(), speedModel.getData(ft.features(), ft.categories()));

					double freespeed = allowedSpeed * speedFactor;

					// Check absolute bounds on the freespeed
					if (speedFactorBounds[0] < 0 && freespeed < -speedFactorBounds[0]/3.6) {
						freespeed = -speedFactorBounds[0]/3.6;
						speedFactor = freespeed / allowedSpeed;
					}
					if (speedFactorBounds[1] < 0 && freespeed > -speedFactorBounds[1]/3.6) {
						freespeed = -speedFactorBounds[1]/3.6;
						speedFactor = freespeed / allowedSpeed;
					}

					link.setFreespeed(freespeed);
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
		features = readFeatures(input.getPath("features.csv"), network.getLinks());

		CSVPrinter csv;
		Path out = output.getPath("eval.csv");
		if (Files.exists(out)) {
			csv = new CSVPrinter(Files.newBufferedWriter(out, StandardOpenOption.APPEND), CSVFormat.DEFAULT);
		} else {
			csv = new CSVPrinter(Files.newBufferedWriter(out, StandardOpenOption.CREATE_NEW), CSVFormat.DEFAULT);
			csv.printRecord("network", "package", "name", "params", "mae", "rmse");
			csv.flush();
		}


		if (evalDetailed)
			Files.createDirectories(Path.of("freespeed_detailed"));


		log.info("Model score:");
		Result r = applyAndEvaluateParams(network, model, validationSet, features, speedFactorBounds, new NetworkParams(0), save(getParamsName(null)));
		writeResult(csv, null, r);

		if (params != null) {
			log.info("Model with parameter score:");
			r = applyAndEvaluateParams(network, model, validationSet, features, speedFactorBounds,
				mapper.readValue(params.toFile(), NetworkParams.class), save(getParamsName(params)));
			writeResult(csv, params, r);
		}

		csv.close();

		if (evalFactors) {
			log.info("Evaluating free-speed factors");
			evalSpeedFactors(output.getPath(), save("best_urban_factor"));
		}

		return 0;
	}

	private String save(String postfix) {
		if (!evalDetailed)
			return null;

		Path base = Path.of("freespeed_detailed");
		return base.resolve(FilenameUtils.getBaseName(input.getNetworkPath()) + "_" + modelClazz.getSimpleName() + "_" + postfix).toString();
	}

	private void evalSpeedFactors(Path eval, String save) throws IOException {

		CSVPrinter csv;
		if (Files.exists(eval)) {
			csv = new CSVPrinter(Files.newBufferedWriter(eval, StandardOpenOption.APPEND), CSVFormat.DEFAULT);
		} else {
			csv = new CSVPrinter(Files.newBufferedWriter(eval, StandardOpenOption.CREATE_NEW), CSVFormat.DEFAULT);
			csv.printRecord("network", "urban_speed_factor", "mae", "rmse");
			csv.flush();
		}

		String networkName = FilenameUtils.getName(input.getNetworkPath());

		NetworkParams best = null;
		double bestScore = Double.POSITIVE_INFINITY;
		double[] bounds = {0, 1};

		for (int i = 25; i <= 100; i++) {
			NetworkParams req = new NetworkParams(i / 100d);
			Result res = applyAndEvaluateParams(network, model, validationSet, features, bounds, req, null);
			csv.printRecord(networkName, i / 100d, res.mae(), res.rmse());
			if (best == null || res.mae() < bestScore) {
				best = req;
				bestScore = res.mae();
			}
		}

		log.info("Best factor {} with mae {}", best.f, bestScore);

		if (save != null) {
			applyAndEvaluateParams(network, model, validationSet, features, bounds, best, save);
		}

		csv.close();
	}

	private String getParamsName(Path params) {
		String pName = paramsName != null ? paramsName : (params != null ? params.getFileName().toString() : "null");
		return params != null ? pName : ("non-optimized_" + pName);
	}

	private void writeResult(CSVPrinter csv, Path params, Result r) throws IOException {
		String network = FilenameUtils.getBaseName(input.getNetworkPath());

		csv.printRecord(network, modelClazz.getPackageName(), modelClazz.getSimpleName(), getParamsName(params), r.mae(), r.rmse());
	}

}
