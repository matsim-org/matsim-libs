package org.matsim.application.analysis.traffic.traveltime;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.*;
import org.matsim.application.prepare.network.SampleNetwork;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@CommandLine.Command(
	name = "sample-validation-routes",
	description = "Sample routes for travel time validation and fetch information from online API.",
	showDefaultValues = true
)
@CommandSpec(
	requireNetwork = true,
	produces = {"routes-validation.csv"}
)
public class SampleValidationRoutes implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(SampleValidationRoutes.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(SampleValidationRoutes.class);
	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(SampleValidationRoutes.class);

	@CommandLine.Mixin
	private ShpOptions shp;

	@CommandLine.Mixin
	private CrsOptions crs;

	@CommandLine.Option(names = "--api", description = "Mapping of [name]=[key] with APIs to retrieve. Available: ${COMPLETION-CANDIDATES}")
	private Map<RouteAPI, String> apis;

	@CommandLine.Option(names = {"-n", "--num-routes"}, description = "Number of routes (per time bin)", defaultValue = "1000")
	private int numRoutes;

	@CommandLine.Option(names = "--hours", description = "Hours to validate", defaultValue = "3,7,8,9,12,13,16,17,18,21", split = ",")
	private List<Integer> hours;

	@CommandLine.Option(names = "--dist-range", description = "Range for the sampled distances.", split = ",", defaultValue = "3000,10000")
	private List<Double> distRange;

	@CommandLine.Option(names = "--exclude-roads", description = "Regexp pattern of road types to exclude as start- and endpoints.", defaultValue = ".+_link")
	private String excludeRoads;

	@CommandLine.Option(names = "--mode", description = "Mode to validate", defaultValue = TransportMode.car)
	private String mode;

	@CommandLine.Option(names = "--input-od", description = "Use input fromNode,toNode instead of sampling", required = false)
	private String inputOD;

	public static void main(String[] args) {
		new SampleValidationRoutes().execute(args);
	}

	/**
	 * Read the produced API files and collect the speeds by hour.
	 */
	public static Map<FromToNodes, Int2ObjectMap<DoubleList>> readValidation(List<String> validationFiles) throws IOException {

		// entry to hour and list of speeds
		Map<FromToNodes, Int2ObjectMap<DoubleList>> entries = new LinkedHashMap<>();

		for (String file : validationFiles) {

			log.info("Loading {}", file);

			try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(file)),
				CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

				for (CSVRecord r : parser) {
					FromToNodes e = new FromToNodes(Id.createNodeId(r.get("from_node")), Id.createNodeId(r.get("to_node")));
					double speed = Double.parseDouble(r.get("dist")) / Double.parseDouble(r.get("travel_time"));

					if (!Double.isFinite(speed)) {
						log.warn("Invalid entry {}", r);
						continue;
					}

					Int2ObjectMap<DoubleList> perHour = entries.computeIfAbsent(e, (k) -> new Int2ObjectLinkedOpenHashMap<>());
					perHour.computeIfAbsent(Integer.parseInt(r.get("hour")), k -> new DoubleArrayList()).add(speed);
				}
			}
		}

		return entries;
	}

	@Override
	@SuppressWarnings({"IllegalCatch", "NestedTryDepth"})
	public Integer call() throws Exception {

		if (apis == null || apis.isEmpty()) {
			log.warn("No API selected. You can use multiple APIs by giving the name and key --api google=[your key]]. Available: {}",
				Arrays.toString(RouteAPI.values()));
			return 2;
		}

		Network network = input.getNetwork();

		SplittableRandom rnd = new SplittableRandom(0);

		FreeSpeedTravelTime tt = new FreeSpeedTravelTime();
		OnlyTimeDependentTravelDisutility util = new OnlyTimeDependentTravelDisutility(tt);
		LeastCostPathCalculator router = new SpeedyALTFactory().createPathCalculator(network, util, tt);


		List<Route> routes;
		if (inputOD != null) {
			log.info("Using input OD file {}", inputOD);
			routes = queryRoutes(network, router);
		} else {
			routes = sampleRoutes(network, router, rnd);
			log.info("Sampled {} routes in range {}", routes.size(), distRange);
		}

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.getPath()), CSVFormat.DEFAULT)) {
			csv.printRecord("from_node", "to_node", "beeline_dist", "dist", "travel_time", "geometry");
			for (Route route : routes) {
				csv.printRecord(route.fromNode, route.toNode,
					CoordUtils.calcEuclideanDistance(network.getNodes().get(route.fromNode).getCoord(), network.getNodes().get(route.toNode).getCoord()),
					route.dist, route.travelTime,
					String.format(Locale.US, "MULTIPOINT(%.5f %.5f, %.5f %.5f)", route.from.getX(), route.from.getY(), route.to.getX(), route.to.getY()));
			}
		}

		List<String> files = new ArrayList<>();

		log.info("Fetching APIs: {}", apis.keySet());

		// Run all services in parallel
		ExecutorService executor = Executors.newCachedThreadPool();
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		for (Map.Entry<RouteAPI, String> e : apis.entrySet()) {

			Path out = Path.of(output.getPath().toString().replace(".csv", "-api-" + e.getKey() + ".csv"));
			futures.add(
				CompletableFuture.runAsync(new FetchRoutesTask(e.getKey(), e.getValue(), routes, hours, out), executor)
			);

			files.add(out.toString());
		}

		CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		all.join();
		executor.shutdown();

		Map<FromToNodes, Int2ObjectMap<DoubleList>> res = readValidation(files);

		Path ref = Path.of(output.getPath().toString().replace(".csv", "-ref.csv"));

		// Write the reference file
		try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(ref), CSVFormat.DEFAULT)) {
			printer.printRecord("from_node", "to_node", "hour", "min", "max", "mean", "std");

			// Target values
			for (Map.Entry<FromToNodes, Int2ObjectMap<DoubleList>> e : res.entrySet()) {

				Int2ObjectMap<DoubleList> perHour = e.getValue();
				for (Int2ObjectMap.Entry<DoubleList> e2 : perHour.int2ObjectEntrySet()) {

					SummaryStatistics stats = new SummaryStatistics();
					// This is as kmh
					e2.getValue().forEach(v -> stats.addValue(v * 3.6));

					printer.printRecord(e.getKey().fromNode, e.getKey().toNode, e2.getIntKey(),
						stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation());
				}
			}
		}

		log.info("All done.");

		return 0;
	}

	/**
	 * Samples routes from the network.
	 */
	private List<Route> sampleRoutes(Network network, LeastCostPathCalculator router, SplittableRandom rnd) {

		List<Route> result = new ArrayList<>();
		List<Link> links = new ArrayList<>(network.getLinks().values());
		String crs = ProjectionUtils.getCRS(network);

		if (this.crs.getInputCRS() != null)
			crs = this.crs.getInputCRS();

		if (crs == null) {
			throw new IllegalArgumentException("Input CRS could not be detected. Please specify with --input-crs [EPSG:xxx]");
		}

		GeotoolsTransformation ct = new GeotoolsTransformation(crs, "EPSG:4326");

		ShpOptions.Index index = shp.isDefined() ? shp.createIndex(crs, "_") : null;
		Predicate<Link> exclude = excludeRoads != null && !excludeRoads.isBlank() ? new Predicate<>() {
			final Pattern p = Pattern.compile(excludeRoads, Pattern.CASE_INSENSITIVE);

			@Override
			public boolean test(Link link) {
				return p.matcher(NetworkUtils.getHighwayType(link)).find();
			}
		} : link -> false;

		for (int i = 0; i < numRoutes; i++) {
			if (links.isEmpty()) {
				log.warn("Not enough route samples could be generated");
				break;
			}

			Link link = links.remove(rnd.nextInt(0, links.size()));

			if (index != null && !index.contains(link.getCoord())) {
				i--;
				continue;
			}

			if (!link.getAllowedModes().contains(mode) || exclude.test(link)) {
				i--;
				continue;
			}

			Coord dest = SampleNetwork.rndCoord(rnd, rnd.nextDouble(distRange.get(0), distRange.get(1)), link);
			Link to = NetworkUtils.getNearestLink(network, dest);

			if (!to.getAllowedModes().contains(mode) || exclude.test(to)) {
				// add the origin link back
				links.add(link);

				i--;
				continue;
			}

			LeastCostPathCalculator.Path path = router.calcLeastCostPath(link.getFromNode(), to.getToNode(), 0, null, null);

			if (path.nodes.size() < 2) {
				i--;
				continue;
			}

			result.add(new Route(
				link.getFromNode().getId(),
				to.getToNode().getId(),
				ct.transform(link.getFromNode().getCoord()),
				ct.transform(to.getToNode().getCoord()),
				path.travelTime,
				path.links.stream().mapToDouble(Link::getLength).sum()
			));
		}
		return result;
	}

	/**
	 * Use given od pairs as input for validation.
	 */
	private List<Route> queryRoutes(Network network, LeastCostPathCalculator router) {

		List<Route> result = new ArrayList<>();
		String crs = ProjectionUtils.getCRS(network);

		if (this.crs.getInputCRS() != null)
			crs = this.crs.getInputCRS();

		if (crs == null) {
			throw new IllegalArgumentException("Input CRS could not be detected. Please specify with --input-crs [EPSG:xxx]");
		}

		GeotoolsTransformation ct = new GeotoolsTransformation(crs, "EPSG:4326");

		try (CSVParser parser = CSVParser.parse(IOUtils.getBufferedReader(inputOD), CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).
			setDelimiter(CsvOptions.detectDelimiter(inputOD)).build())) {

			List<String> header = parser.getHeaderNames();
			if (!header.contains("from_node"))
				throw new IllegalArgumentException("Missing 'from_node' column in input file");
			if (!header.contains("to_node"))
				throw new IllegalArgumentException("Missing 'to_node' column in input file");

			for (CSVRecord r : parser) {
				Node fromNode = network.getNodes().get(Id.createNodeId(r.get("from_node")));
				Node toNode = network.getNodes().get(Id.createNodeId(r.get("to_node")));

				if (fromNode == null)
					throw new IllegalArgumentException("Node " + r.get("from_node") + " not found");
				if (toNode == null)
					throw new IllegalArgumentException("Node " + r.get("to_node") + " not found");

				LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, 0, null, null);
				result.add(new Route(
					fromNode.getId(),
					toNode.getId(),
					ct.transform(fromNode.getCoord()),
					ct.transform(toNode.getCoord()),
					path.travelTime,
					path.links.stream().mapToDouble(Link::getLength).sum()
				));
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return result;
	}

	/**
	 * Key as pair of from and to node.
	 */
	public record FromToNodes(Id<Node> fromNode, Id<Node> toNode) {
	}

	record Route(Id<Node> fromNode, Id<Node> toNode, Coord from, Coord to, double travelTime, double dist) {
	}

}
