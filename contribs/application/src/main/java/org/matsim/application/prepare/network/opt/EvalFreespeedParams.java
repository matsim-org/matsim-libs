package org.matsim.application.prepare.network.opt;

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

import static org.matsim.application.prepare.network.opt.NetworkParamsOpt.*;

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

	private Network network;
	private NetworkModel model;
	private Object2DoubleMap<SampleValidationRoutes.FromToNodes> validationSet;
	private Map<Id<Link>, Feature> features;

	public static void main(String[] args) {
		new EvalFreespeedParams().execute(args);
	}

	static Result applyAndEvaluateParams(
		Network network, NetworkModel model, Object2DoubleMap<SampleValidationRoutes.FromToNodes> validationSet, Map<Id<Link>, Feature> features,
		double[] speedFactorBounds, Request request, String save) throws IOException {

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
						speedFactor = speedModel.predict(ft.features(), p);
					} else
						speedFactor = speedModel.predict(ft.features());

					// apply lower and upper bound
					speedFactor = Math.max(speedFactorBounds[0], speedFactor);
					speedFactor = Math.min(speedFactorBounds[1], speedFactor);

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

		CSVPrinter csv;
		Path out = output.getPath("eval.csv");
		if (Files.exists(out)) {
			csv = new CSVPrinter(Files.newBufferedWriter(out, StandardOpenOption.APPEND), CSVFormat.DEFAULT);
		} else {
			csv = new CSVPrinter(Files.newBufferedWriter(out, StandardOpenOption.CREATE_NEW), CSVFormat.DEFAULT);
			csv.printRecord("network", "package", "name", "params", "mae", "rmse");
			csv.flush();
		}

		log.info("Model score:");
		Result r = applyAndEvaluateParams(network, model, validationSet, features, speedFactorBounds, new Request(0), null);
		writeResult(csv, null, r);

		if (params != null) {
			log.info("Model with parameter score:");
			r = applyAndEvaluateParams(network, model, validationSet, features, speedFactorBounds,
				mapper.readValue(params.toFile(), Request.class), null);
			writeResult(csv, params, r);
		}

		csv.close();

		if (evalFactors) {
			log.info("Evaluating free-speed factors");
			evalSpeedFactors(output.getPath());
		}

		return 0;
	}

	private void evalSpeedFactors(Path eval) throws IOException {

		CSVPrinter csv;
		if (Files.exists(eval)) {
			csv = new CSVPrinter(Files.newBufferedWriter(eval, StandardOpenOption.APPEND), CSVFormat.DEFAULT);
		} else {
			csv = new CSVPrinter(Files.newBufferedWriter(eval, StandardOpenOption.CREATE_NEW), CSVFormat.DEFAULT);
			csv.printRecord("network", "urban_speed_factor", "mae", "rmse");
			csv.flush();
		}

		String networkName = FilenameUtils.getName(input.getNetworkPath());

		for (int i = 25; i <= 100; i++) {
			Result res = applyAndEvaluateParams(network, model, validationSet, features, new double[]{0, 1},
				new Request(i / 100d), null);
			csv.printRecord(networkName, i / 100d, res.mae(), res.rmse());
		}


		csv.close();
	}

	private void writeResult(CSVPrinter csv, Path params, Result r) throws IOException {
		String pName = paramsName != null ? paramsName : params.getFileName().toString();
		String p = params != null ? pName : "non_optimized_" + pName;
		String network = FilenameUtils.getName(input.getNetworkPath());

		csv.printRecord(network, modelClazz.getPackageName(), modelClazz.getSimpleName(), p, r.mae(), r.rmse());
	}


}
