package org.matsim.application.prepare.network;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
	name = "sample-network",
	description = "Sample intersections, links and routes from network"
)
@CommandSpec(requireNetwork = true, produces = {"intersections.txt", "edges.txt", "routes.txt"})
public class SampleNetwork implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(SampleNetwork.class);

	@CommandLine.Mixin
	private InputOptions input = InputOptions.ofCommand(SampleNetwork.class);

	@CommandLine.Mixin
	private OutputOptions output = OutputOptions.ofCommand(SampleNetwork.class);

	@CommandLine.Mixin
	private ShpOptions shp;

	@CommandLine.Mixin
	private CrsOptions crs;

	@CommandLine.Option(names = "--sample-size", description = "Number of samples to collect for each category.", defaultValue = "5000")
	private int sample;

	@CommandLine.Option(names = "--filter", description = "Remove certain road types")
	private String typeFilter;

	@CommandLine.Option(names = "--routes-size", description = "Number of routes to collect.", defaultValue = "5000")
	private int routesSize;

	@CommandLine.Option(names = "--routes-dist", description = "Avg. routes distance.", defaultValue = "6000")
	private int routesDist;

	public static void main(String[] args) {
		new SampleNetwork().execute(args);
	}

	/**
	 * Random coord in the same direction as a link.
	 */
	public static Coord rndCoord(SplittableRandom rnd, double dist, Link link) {

		Coord v = link.getFromNode().getCoord();
		Coord u = link.getToNode().getCoord();

		var angle = Math.atan2(u.getY() - v.getY(), u.getX() - v.getX());

		var sample = angle + rnd.nextDouble(-0.2, 0.2) * Math.PI * 2;

		var x = Math.cos(sample) * dist;
		var y = Math.sin(sample) * dist;

		return CoordUtils.round(new Coord(v.getX() + x, v.getY() + y));
	}

	/**
	 * Skip certain nodes to improve class imbalance regarding the allowed speed.
	 */
	private static double skip(Node node, String key) {

		// all traffic lights are considered
		if (key.equals("traffic_light"))
			return 1;

		Optional<? extends Link> first = node.getInLinks().values().stream().findFirst();
		if (first.isEmpty())
			return 0;

		Link link = first.get();

		// very long or short links are skipped
		if (link.getLength() > 500 || link.getLength() < 15)
			return 0;

		double skip = 1;
		if (NetworkUtils.getAllowedSpeed(link) == 13.89)
			skip = 0.6;
		else if (NetworkUtils.getAllowedSpeed(link) == 8.33)
			skip = 0.3;

		// Increase samples with more than 1 lane
		if (link.getNumberOfLanes() == 1)
			skip *= 0.7;

		return skip;
	}

	@Override
	public Integer call() throws Exception {

		Network network = input.getNetwork();

		Map<String, ? extends List<? extends Node>> byType = network.getNodes().values().stream().collect(Collectors.groupingBy(
			n -> (String) n.getAttributes().getAttribute("type"), Collectors.toList()
		));

		SplittableRandom rnd = new SplittableRandom(0);

		try (BufferedWriter intersections = Files.newBufferedWriter(output.getPath("intersections.txt"))) {

			for (Map.Entry<String, ? extends List<? extends Node>> e : byType.entrySet()) {

				List<? extends Node> list = e.getValue();

				// for now only consider traffic lights
				if (!e.getKey().equals("traffic_light"))
					continue;

				log.info("Sampling {} out of {} intersections for type {}", sample, list.size(), e.getKey());

				for (int i = 0; i < sample && !list.isEmpty(); i++) {

					Node n = list.remove(rnd.nextInt(0, list.size()));

					// leave out certain links
					if (rnd.nextDouble() > skip(n, e.getKey())) {
						i--;
						continue;
					}

					intersections.write(n.getId().toString() + "\n");
				}
			}
		}

		Map<Double, ? extends List<? extends Link>> bySpeed = network.getLinks().values().stream()
			.filter(l -> !"traffic_light".equals(l.getToNode().getAttributes().getAttribute("type")))
			.filter(l -> l.getLength() < 500 && l.getLength() > 50)
			.collect(Collectors.groupingBy(
				n -> (Double) n.getAttributes().getAttribute("allowed_speed"), Collectors.toList()
			));

		try (BufferedWriter links = Files.newBufferedWriter(output.getPath("edges.txt"))) {

			for (Map.Entry<Double, ? extends List<? extends Link>> e : bySpeed.entrySet()) {

				List<? extends Link> list = e.getValue();

				if (list.size() < 50)
					continue;

				log.info("Sampling {} out of {} links for speed {}", sample / 10, list.size(), e.getKey());

				// Use longest link segments
				list.sort(Comparator.comparingDouble(l -> -l.getLength()));

				for (int i = 0; i < sample / 10 && i < list.size(); i++) {
					Link link = list.get(i);
					links.write(link.getId().toString() + "\n");
				}
			}
		}

		Network cityNetwork = createCityNetwork(network);

		RandomizedTravelTime tt = new RandomizedTravelTime(rnd);

		LeastCostPathCalculator router = createRandomizedRouter(cityNetwork, tt);

		sampleCityRoutes(cityNetwork, router, tt, rnd);

		return 0;
	}

	/**
	 * Samples routes from the network.
	 */
	private void sampleCityRoutes(Network network, LeastCostPathCalculator router, RandomizedTravelTime tt, SplittableRandom rnd) throws IOException {

		List<? extends Link> links = new ArrayList<>(network.getLinks().values());

		GeometryFactory f = new GeometryFactory();
		WKTWriter w = new WKTWriter();
		w.setPrecisionModel(new PrecisionModel(1));

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(output.getPath("routes.txt")), CSVFormat.DEFAULT)) {

			csv.printRecord("fromEdge", "toEdge", "min_capacity", "travel_time", "geometry");

			for (int i = 0; i < routesSize; i++) {

				Link link = links.get(rnd.nextInt(0, links.size()));

				Coord dest = rndCoord(rnd, routesDist, link);

				Link to = NetworkUtils.getNearestLink(network, dest);

				// Links could be on the very edge so that nodes are outside the network
				if (to == null || !network.getNodes().containsKey(link.getFromNode().getId()) ||
					!network.getNodes().containsKey(to.getToNode().getId())) {
					i--;
					continue;
				}

				LeastCostPathCalculator.Path path = router.calcLeastCostPath(link.getFromNode(), to.getToNode(), 0, null, null);

				if (path == null || path.nodes.size() < 2) {
					i--;
					continue;
				}

				double minCapacity = path.links.stream().mapToDouble(Link::getCapacity).min().orElse(-1);

				LineString lineString = f.createLineString(path.nodes.stream().map(n -> MGC.coord2Point(n.getCoord()).getCoordinate()).toArray(Coordinate[]::new));

				Polygon polygon = (Polygon) lineString.buffer(100);

				Polygon simplified = (Polygon) TopologyPreservingSimplifier.simplify(polygon, 30);

				csv.print(link.getId());
				csv.print(path.links.get(path.links.size() - 1).getId());
				csv.print(minCapacity);
				csv.print(path.travelTime);
				csv.print(w.write(simplified));

				csv.println();

				// Reset randomness
				tt.reset();
			}

		}
	}

	/**
	 * Create network without highways.
	 */
	private Network createCityNetwork(Network network) {

		// no filtering needed
		if (typeFilter == null && !shp.isDefined())
			return network;

		NetworkFilterManager filter = new NetworkFilterManager(network, new NetworkConfigGroup());
		if (typeFilter != null)
			filter.addLinkFilter(l -> !NetworkUtils.getHighwayType(l).startsWith(typeFilter));

		if (shp.isDefined()) {

			String crs = ProjectionUtils.getCRS(network);
			if (this.crs.getInputCRS() != null)
				crs = this.crs.getInputCRS();
			if (crs == null) {
				throw new IllegalArgumentException("Input CRS could not be detected. Please specify with --input-crs [EPSG:xxx]");
			}

			ShpOptions.Index index = shp.createIndex(crs, "_");
			filter.addLinkFilter(l -> index.contains(l.getCoord()));
		}

		Network net = filter.applyFilters();

		MultimodalNetworkCleaner cleaner = new MultimodalNetworkCleaner(net);
		cleaner.run(Set.of(TransportMode.car));

		return net;
	}

	/**
	 * Router with randomization.
	 */
	private LeastCostPathCalculator createRandomizedRouter(Network network, TravelTime tt) {

		OnlyTimeDependentTravelDisutility util = new OnlyTimeDependentTravelDisutility(tt);
		return new DijkstraFactory(false).createPathCalculator(network, util, tt);
	}

	private static final class RandomizedTravelTime implements TravelTime {

		private final Object2DoubleMap<Link> factors = new Object2DoubleOpenHashMap<>();

		private final SplittableRandom rnd;

		RandomizedTravelTime(SplittableRandom rnd) {
			this.rnd = rnd;
		}

		void reset() {
			factors.clear();
		}

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			String type = NetworkUtils.getHighwayType(link);

			double f = factors.computeIfAbsent(link, l -> rnd.nextDouble(0.5, 1.5));
			// Main roads are penalized
			if (type.startsWith("primary") || type.startsWith("secondary"))
				f = 1.5;
			else if (type.startsWith("motorway"))
				f = 2;

			double speed = link.getLength() / Math.max(link.getFreespeed(time), 8.3);
			return speed * f;
		}
	}

}
