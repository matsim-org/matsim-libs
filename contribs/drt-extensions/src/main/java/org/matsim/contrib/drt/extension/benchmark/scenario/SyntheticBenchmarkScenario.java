/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.benchmark.scenario;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.extension.DrtWithExtensionsConfigGroup;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import static org.matsim.core.config.groups.ControllerConfigGroup.RoutingAlgorithmType.CHRouter;

/**
 * Creates synthetic DRT benchmark scenarios, optionally using an external network.
 * <p>
 * If a {@code networkUrl} is provided (file path or HTTP URL), that network is loaded
 * instead of generating a synthetic grid.  The population sigma is then derived from
 * the actual network extent (¼ of the diagonal).
 * <p>
 * Example – synthetic grid:
 * <pre>
 * Controler c = SyntheticBenchmarkScenario.builder()
 *     .agents(50_000)
 *     .vehicles(500)
 *     .build();
 * </pre>
 * Example – Berlin v7 network:
 * <pre>
 * Controler c = SyntheticBenchmarkScenario.builder()
 *     .agents(50_000)
 *     .vehicles(500)
 *     .networkUrl("https://svn.vsp.tu-berlin.de/.../berlin-v7.0-network.xml.gz")
 *     .build();
 * </pre>
 *
 * @author Steffen Axer
 */
public class SyntheticBenchmarkScenario {
	private static final Logger LOG = LogManager.getLogger(SyntheticBenchmarkScenario.class);

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private int agents = 10_000;
		private int vehicles = 100;
		private int gridSize = 100;
		private double cellSize = 100.0;
		private String outputDirectory = "output/benchmark";
		private boolean timeDependent = true;
		private int vehicleCapacity = 4;
		/** Optional: URL or file path to an external MATSim network. */
		private String networkUrl = null;
		/** Cell size [m] for DVRP travel-time matrix square-grid zones. */
		private double matrixCellSize = 200.0;
		/** Insertion search params (default: SelectiveInsertionSearchParams). */
		private DrtInsertionSearchParams insertionSearchParams = new SelectiveInsertionSearchParams();

		public Builder agents(int n) { this.agents = n; return this; }
		public Builder vehicles(int n) { this.vehicles = n; return this; }
		public Builder gridSize(int n) { this.gridSize = n; return this; }
		public Builder cellSize(double d) { this.cellSize = d; return this; }
		public Builder outputDirectory(String s) { this.outputDirectory = s; return this; }
		public Builder timeDependent(boolean b) { this.timeDependent = b; return this; }
		public Builder vehicleCapacity(int n) { this.vehicleCapacity = n; return this; }
		/** Load an external network from a file path or HTTP/HTTPS URL instead of generating a grid. */
		public Builder networkUrl(String url) { this.networkUrl = url; return this; }
		public Builder matrixCellSize(double size) { this.matrixCellSize = size; return this; }
		/** Set the insertion search params (e.g., ExtensiveInsertionSearchParams). Default: SelectiveInsertionSearchParams. */
		public Builder insertionSearchParams(DrtInsertionSearchParams params) { this.insertionSearchParams = params; return this; }

		public Controler build() {
			Path outputPath = Path.of(outputDirectory).toAbsolutePath();
			Path inputPath = outputPath.getParent().resolve(outputPath.getFileName() + "_input");
			try {
				Files.createDirectories(inputPath);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create input directory: " + inputPath, e);
			}

			Config config = createConfig(outputPath);
			Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);

			double populationSigma;
			if (networkUrl != null && !networkUrl.isBlank()) {
				// ---- Load external network ----
				LOG.info("Loading external network from: {}", networkUrl);
				Path networkPath = resolveNetwork(networkUrl, inputPath);
				new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());
				populationSigma = computeSigmaFromNetwork(scenario.getNetwork());
				LOG.info("External network loaded: {} nodes, {} links – population sigma = {} m",
					scenario.getNetwork().getNodes().size(),
					scenario.getNetwork().getLinks().size(),
					(long) populationSigma);
			} else {
				// ---- Generate synthetic grid ----
				LOG.info("Generating synthetic {}x{} grid network", gridSize, gridSize);
				new GridNetworkGenerator(gridSize, cellSize, 50.0 / 3.6, 1800.0, 2.0).generate(scenario);
				populationSigma = gridSize * cellSize / 4.0;
			}

			LOG.info("Creating benchmark: {} agents, {} vehicles", agents, vehicles);
			Path fleetFile = new FleetGenerator(vehicleCapacity, 0, 86400).generate(vehicles, scenario, inputPath);
			MultiModeDrtConfigGroup.get(config).getModalElements().iterator().next()
				.setVehiclesFile(fleetFile.toAbsolutePath().toString());

			new PopulationGenerator(timeDependent, populationSigma).generate(agents, scenario);

			return DrtControlerCreator.createControler(config, scenario, false);
		}

		/**
		 * Resolves a network from a file path or HTTP URL.
		 * For HTTP/HTTPS URLs the file is downloaded to {@code inputPath/network.xml.gz}.
		 */
		private static Path resolveNetwork(String url, Path inputPath) {
			if (url.startsWith("http://") || url.startsWith("https://")) {
				String fileName = url.substring(url.lastIndexOf('/') + 1);
				Path localPath = inputPath.resolve(fileName);
				if (!Files.exists(localPath)) {
					LOG.info("Downloading network from {} ...", url);
					try {
						HttpClient client = HttpClient.newBuilder()
							.connectTimeout(Duration.ofSeconds(30))
							.followRedirects(HttpClient.Redirect.NORMAL)
							.build();
						HttpRequest request = HttpRequest.newBuilder()
							.uri(URI.create(url))
							.timeout(Duration.ofMinutes(5))
							.GET()
							.build();
						HttpResponse<InputStream> response = client.send(request,
							HttpResponse.BodyHandlers.ofInputStream());
						if (response.statusCode() != 200) {
							throw new RuntimeException("Failed to download network (HTTP " + response.statusCode() + "): " + url);
						}
						try (InputStream in = response.body()) {
							Files.copy(in, localPath, StandardCopyOption.REPLACE_EXISTING);
						}
						LOG.info("Network downloaded to {}", localPath);
					} catch (IOException | InterruptedException e) {
						throw new RuntimeException("Failed to download network from: " + url, e);
					}
				} else {
					LOG.info("Using cached network file: {}", localPath);
				}
				return localPath;
			}
			// Local file path
			return Path.of(url);
		}

		/**
		 * Estimates a reasonable population sigma (¼ of the network diagonal).
		 */
		private static double computeSigmaFromNetwork(Network network) {
			double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
			double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
			for (Link link : network.getLinks().values()) {
				double x = link.getCoord().getX(), y = link.getCoord().getY();
				if (x < minX) minX = x;
				if (x > maxX) maxX = x;
				if (y < minY) minY = y;
				if (y > maxY) maxY = y;
			}
			double dx = maxX - minX, dy = maxY - minY;
			return Math.sqrt(dx * dx + dy * dy) / 4.0;
		}

		private Config createConfig(Path outputPath) {
			DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
			SquareGridZoneSystemParams zoneParams = new SquareGridZoneSystemParams();
			zoneParams.setCellSize(matrixCellSize);
			dvrpConfig.getTravelTimeMatrixParams().addParameterSet(zoneParams);
			dvrpConfig.getTravelTimeMatrixParams().setMaxNeighborDistance(0);

			MultiModeDrtConfigGroup multiModeDrtConfig = new MultiModeDrtConfigGroup();
			DrtConfigGroup drtConfig = new DrtWithExtensionsConfigGroup();
			drtConfig.setMode("drt");
			drtConfig.setStopDuration(60.0);

			var constraints = drtConfig.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet();
			constraints.setMaxWaitTime(900);
			constraints.setMaxTravelTimeAlpha(1.5);
			constraints.setMaxTravelTimeBeta(900);
			constraints.setRejectRequestIfMaxWaitOrTravelTimeViolated(false);

			drtConfig.addParameterSet(insertionSearchParams);
			multiModeDrtConfig.addParameterSet(drtConfig);

			Config config = ConfigUtils.createConfig(multiModeDrtConfig, dvrpConfig);
			LOG.info("DVRP TT matrix cell size: {} m", matrixCellSize);
			config.controller().setOutputDirectory(outputPath.toString());
			config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setLastIteration(0);
			config.controller().setWriteEventsInterval(0);
			config.controller().setWritePlansInterval(0);
			config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
			config.qsim().setStartTime(0);
			config.qsim().setEndTime(86400);
			config.replanning().clearStrategySettings();
			config.global().setNumberOfThreads(6);
			config.controller().setRoutingAlgorithmType(CHRouter);
			config.scoring().addModeParams(new ScoringConfigGroup.ModeParams("drt"));
			config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());

			return config;
		}
	}
}
