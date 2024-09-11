package org.matsim.application.prepare.network.params;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.analysis.traffic.traveltime.SampleValidationRoutes;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Private helper class with utility functions.
 */
class NetworkParamsOpt {

	/**
	 * Factor limits. Deviation of 3% above max is allowed.
	 */
	static final String DEFAULT_FACTOR_BOUNDS = "0.25,1.03";

	private NetworkParamsOpt() {
	}

	static NetworkModel load(Class<? extends NetworkModel> modelClazz) {
		try {
			return modelClazz.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("Could not instantiate the network model", e);
		}
	}

	/**
	 * Read network edge features from csv.
	 */
	static Map<Id<Link>, Feature> readFeatures(String input, Map<Id<Link>, ? extends Link> links) throws IOException {

		Map<Id<Link>, Feature> features = new IdMap<>(Link.class, links.size());

		// Create features from link attributes
		for (Link link : links.values()) {
			features.put(link.getId(), createDefaultFeature(link));
		}

		try (CSVParser reader = new CSVParser(IOUtils.getBufferedReader(input),
			CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

			List<String> header = reader.getHeaderNames();

			for (CSVRecord row : reader) {

				Id<Link> id = Id.createLinkId(row.get("linkId"));
				Link link = links.get(id);
				Feature ft = features.computeIfAbsent(id, (k) -> createDefaultFeature(link));

				for (String column : header) {
					String v = row.get(column);
					try {
						ft.features.put(column, Double.parseDouble(v));
					} catch (NumberFormatException e) {
						// every not equal to True will be false
						ft.features.put(column, Boolean.parseBoolean(v) ? 1 : 0);
						ft.categories.put(column, v);
					}
				}

				String highwayType = header.contains(NetworkUtils.TYPE) ? row.get(NetworkUtils.TYPE) :
					(link != null ? NetworkUtils.getHighwayType(link) : null);

				features.put(id, new Feature(row.get("junction_type").intern(), highwayType, ft.features, ft.categories));
			}
		}

		return features;
	}

	/**
	 * Create default feature based on link attributes.
	 */
	private static Feature createDefaultFeature(Link link) {
		Object2DoubleOpenHashMap<String> ft = new Object2DoubleOpenHashMap<>();
		ft.defaultReturnValue(Double.NaN);
		Object2ObjectMap<String, String> categories = new Object2ObjectOpenHashMap<>();

		// Link might not be present in the network
		if (link == null)
			return new Feature("", "", ft, categories);

		String highwayType = NetworkUtils.getHighwayType(link);
		categories.put("highway_type", highwayType);
		ft.put("idx", link.getId().index());
		ft.put("speed", NetworkUtils.getAllowedSpeed(link));
		ft.put("num_lanes", link.getNumberOfLanes());
		ft.put("length", link.getLength());
		ft.put("capacity", link.getCapacity());
		ft.put("freespeed", link.getFreespeed());

		for (Map.Entry<String, Object> e : link.getAttributes().getAsMap().entrySet()) {
			String key = e.getKey();
			Object value = e.getValue();
			if (value instanceof Number) {
				ft.put(key, ((Number) value).doubleValue());
			} else if (value instanceof Boolean) {
				ft.put(key, (Boolean) value ? 1 : 0);
			} else {
				categories.put(key, value.toString());
			}
		}

		return new Feature("", highwayType, ft, categories);
	}

	/**
	 * Read validation files and calc target speed.
	 */
	static Object2DoubleMap<SampleValidationRoutes.FromToNodes> readValidation(List<String> validationFiles, List<Integer> refHours) throws IOException {

		// entry to hour and list of speeds
		Map<SampleValidationRoutes.FromToNodes, Int2ObjectMap<DoubleList>> entries = SampleValidationRoutes.readValidation(validationFiles);

		Object2DoubleMap<SampleValidationRoutes.FromToNodes> result = new Object2DoubleOpenHashMap<>();

		// Target values
		for (Map.Entry<SampleValidationRoutes.FromToNodes, Int2ObjectMap<DoubleList>> e : entries.entrySet()) {

			Int2ObjectMap<DoubleList> perHour = e.getValue();

			double avg = refHours.stream().map(h -> perHour.get((int) h).doubleStream())
				.flatMapToDouble(Function.identity()).average().orElseThrow();


			result.put(e.getKey(), avg);
		}

		return result;
	}

	static Result evaluate(Network network, Object2DoubleMap<SampleValidationRoutes.FromToNodes> validationSet, Map<Id<Link>, Feature> features, Map<Id<Link>, double[]> attributes, String save) throws IOException {
		FreeSpeedTravelTime tt = new FreeSpeedTravelTime();
		OnlyTimeDependentTravelDisutility util = new OnlyTimeDependentTravelDisutility(tt);
		LeastCostPathCalculator router = new DijkstraFactory(false).createPathCalculator(network, util, tt);

		SummaryStatistics rmse = new SummaryStatistics();
		SummaryStatistics mse = new SummaryStatistics();

		CSVPrinter csv = save != null ? new CSVPrinter(Files.newBufferedWriter(Path.of(save + "-eval.csv")), CSVFormat.DEFAULT) : null;

		if (csv != null)
			csv.printRecord("from_node", "to_node", "beeline_dist", "sim_speed", "ref_speed");

		Map<String, List<Data>> data = new HashMap<>();

		for (Object2DoubleMap.Entry<SampleValidationRoutes.FromToNodes> e : validationSet.object2DoubleEntrySet()) {

			SampleValidationRoutes.FromToNodes r = e.getKey();

			Node fromNode = network.getNodes().get(r.fromNode());
			Node toNode = network.getNodes().get(r.toNode());
			LeastCostPathCalculator.Path path = router.calcLeastCostPath(fromNode, toNode, 0, null, null);

			// iterate over the path, calc better correction
			double distance = path.links.stream().mapToDouble(Link::getLength).sum();
			double speed = distance / path.travelTime;

			double correction = speed / e.getDoubleValue();

			for (Link link : path.links) {

				if (!attributes.containsKey(link.getId()))
					continue;

				Feature ft = features.get(link.getId());
				double[] input = attributes.get(link.getId());
				double speedFactor = (double) link.getAttributes().getAttribute("speed_factor");

				data.computeIfAbsent(ft.junctionType(), (k) -> new ArrayList<>())
					.add(new Data(input, speedFactor, speedFactor / correction));
			}


			rmse.addValue(Math.pow(e.getDoubleValue() - speed, 2));
			mse.addValue(Math.abs((e.getDoubleValue() - speed) * 3.6));

			if (csv != null)
				csv.printRecord(r.fromNode(), r.toNode(), (int) CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()),
					speed, e.getDoubleValue());
		}

		if (csv != null)
			csv.close();

		return new Result(rmse.getMean(), mse.getMean(), data);
	}

	record Feature(String junctionType, String highwayType, Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
	}

	record Result(double rmse, double mae, Map<String, List<Data>> data) {
	}

	record Data(double[] x, double yPred, double yTrue) {
	}

}
