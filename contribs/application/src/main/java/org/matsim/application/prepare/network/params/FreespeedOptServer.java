package org.matsim.application.prepare.network.params;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.impl.bootstrap.HttpServer;
import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap;
import org.apache.hc.core5.http.io.HttpRequestHandler;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.traffic.traveltime.SampleValidationRoutes;
import org.matsim.application.options.InputOptions;
import org.matsim.application.prepare.network.params.NetworkParamsOpt.Feature;
import org.matsim.application.prepare.network.params.NetworkParamsOpt.Result;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This class runs a server that receives model parameters for free speed.
 * The corresponding optimizer is part of `matsim-python-tools` package.
 * See <a href="https://github.com/matsim-vsp/matsim-python-tools/blob/v0.0.18/matsim/scenariogen/network/run_opt_freespeed.py">this code</a>.
 * More about the method can be found in the paper "Road network free flow speed estimation using microscopic simulation and point-to-point travel times"; ABMTRANS 2024.
 */
@CommandLine.Command(
	name = "freespeed-opt-server",
	description = "Start server for freespeed optimization."
)
@CommandSpec(
	requireNetwork = true,
	requires = "features.csv"
)
public class FreespeedOptServer implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(FreespeedOptServer.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(FreespeedOptServer.class);

	@CommandLine.Option(names = "--model", description = "Reference to the network model class", required = true)
	private Class<? extends NetworkModel> modelClazz;

	@CommandLine.Parameters(arity = "1..*", description = "Input validation files loaded from APIs")
	private List<String> validationFiles;

	@CommandLine.Option(names = "--factor-bounds", split = ",", description = "Speed factor limits (lower, upper bound). " +
		"Can be negative to indicate absolute speed bounds (in km/h)", defaultValue = NetworkParamsOpt.DEFAULT_FACTOR_BOUNDS)
	private double[] speedFactorBounds;

	@CommandLine.Option(names = "--ref-hours", description = "Reference hours", defaultValue = "3,21", split = ",")
	private List<Integer> refHours;

	@CommandLine.Option(names = "--port", description = "Port for the server", defaultValue = "9090")
	private int port;

	private Network network;
	private NetworkModel model;
	private Object2DoubleMap<SampleValidationRoutes.FromToNodes> validationSet;
	private Map<Id<Link>, Feature> features;

	private ObjectMapper mapper;

	/**
	 * Original speeds.
	 */
	private Object2DoubleMap<Id<Link>> speeds = new Object2DoubleOpenHashMap<>();

	public static void main(String[] args) {
		new FreespeedOptServer().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		try {
			model = modelClazz.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			log.error("Could not instantiate the network model", e);
			return 2;
		}

		network = input.getNetwork();
		mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);

		for (Link link : network.getLinks().values()) {
			speeds.put(link.getId(), link.getFreespeed());
		}

		validationSet = NetworkParamsOpt.readValidation(validationFiles, refHours);
		features = NetworkParamsOpt.readFeatures(input.getPath("features.csv"), network.getLinks());

		log.info("Initial score:");
		applyAndEvaluateParams(null, "init");

		Backend backend = new Backend();
		try (HttpServer server = ServerBootstrap.bootstrap()
			.setListenerPort(port)
			.setExceptionListener(backend)
			.register("/", backend)
			.create()) {

			server.start();

			log.info("Server running on {}", port);

			server.awaitTermination(TimeValue.MAX_VALUE);
		}

		return 0;
	}

	private Result applyAndEvaluateParams(NetworkParams request, String save) throws IOException {
		return EvalFreespeedParams.applyAndEvaluateParams(network, model, validationSet, features, speedFactorBounds,
			request, save);
	}

	private final class Backend implements HttpRequestHandler, ExceptionListener {

		@Override
		public void handle(ClassicHttpRequest request, ClassicHttpResponse response, HttpContext context) throws IOException {

			NetworkParams req = mapper.readValue(request.getEntity().getContent(), NetworkParams.class);

			Result stats = applyAndEvaluateParams(req, null);

			response.setCode(200);

			String result = mapper.writeValueAsString(stats);
			response.setEntity(HttpEntities.create(result, ContentType.APPLICATION_JSON));
		}

		@Override
		public void onError(Exception ex) {
			log.error("Error during request", ex);
		}

		@Override
		public void onError(HttpConnection connection, Exception ex) {
			log.error("Error during request {}", connection, ex);
		}
	}

}
