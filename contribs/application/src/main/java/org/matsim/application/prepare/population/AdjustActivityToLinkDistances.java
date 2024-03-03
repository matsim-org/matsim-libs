package org.matsim.application.prepare.population;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

@CommandLine.Command(
		name = "adjust-activity-to-link-distances",
		description = "Ensure activity to nearest link distance outside shape file is similar to within shape file."
)
public class AdjustActivityToLinkDistances implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AdjustActivityToLinkDistances.class);

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Input population xml")
	private List<Path> input;

	@CommandLine.Option(names = "--network", description = "Network xml", required = true)
	private Path networkPath;

	@CommandLine.Option(names = "--output", description = "Output population xml", required = true)
	private Path output;

	@CommandLine.Option(names = "--scale", description = "Additional scale for the length", defaultValue = "1")
	private double scale;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	public static void main(String[] args) {
		new AdjustActivityToLinkDistances().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		if (shp.getShapeFile() == null) {
			log.error("Shp file is required as input");
			return 2;
		}

		Population population = PopulationUtils.readPopulation(input.get(0).toString());
		Network network = NetworkUtils.readNetwork(networkPath.toString());

		ShpOptions.Index index = shp.createIndex(crs.getInputCRS(), "_");

		DoubleList dist = computeLinkDistances(population, network, act -> index.contains(act.getCoord()));

		// Data exploring
		/*
		try (BufferedWriter writer = Files.newBufferedWriter(Path.of("dist.csv"))) {
			for (Double d : dist) {
				writer.write(d + "\n");
			}
		}
		 */

		double mean = dist.doubleStream().sum() / dist.size();
		double std = Math.sqrt(dist.doubleStream().map(x -> (x - mean) * (x - mean)).sum() / dist.size());

		log.info("Distance to nearest link distribution within shape has mean: {}, std: {}", mean, std);

		double lmean = dist.doubleStream().map(Math::log).sum() / dist.size();
		double lstd = Math.sqrt(dist.doubleStream().map(Math::log).map(x -> (x - lmean) * (x - lmean)).sum() / dist.size());

		LogNormalDistribution normal = new LogNormalDistribution(lmean, lstd);
		Random r = new Random(1234);

		dist.clear();

		// same coordinates always need to be mapped to same position
		Map<Coord, Coord> mapping = new HashMap<>();

		for (Person p : population.getPersons().values()) {

			Plan plan = p.getSelectedPlan();

			for (Activity act : PopulationUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {

				if (!index.contains(act.getCoord())) {

					Link link = NetworkUtils.getNearestLinkExactly(network, act.getCoord());
					Coord coord = NetworkUtils.findNearestPointOnLink(act.getCoord(), link);
					double d = CoordUtils.calcEuclideanDistance(act.getCoord(), coord);

					dist.add(d);

					Coord v;
					if (mapping.containsKey(act.getCoord()))
						v = mapping.get(act.getCoord());
					else {

						// Compute perpendicular vector and normalize length to 1
						double x = link.getFromNode().getCoord().getX() - link.getToNode().getCoord().getX();
						double y = link.getFromNode().getCoord().getY() - link.getToNode().getCoord().getY();

						double length = Math.sqrt(x*x + y*y);
						x /= length;
						y /= length;

						double m = scale * Math.max(0, normal.sample());

						// Random direction
						if (r.nextBoolean()) {
							v = new Coord(coord.getX() + y * m, coord.getY() - x * m);
						} else {
							v = new Coord(coord.getX() - y * m, coord.getY() + x * m);
						}

						v = CoordUtils.round(v);

						mapping.put(act.getCoord(), v);
					}

					act.setCoord(v);
				}
			}
		}

		double mean2 = dist.doubleStream().sum() / dist.size();
		double std2 = Math.sqrt(dist.doubleStream().map(x -> (x - mean2) * (x - mean2)).sum() / dist.size());

		log.info("Distance to nearest link distribution outside shape had mean: {}, std: {}", mean2, std2);

		dist = computeLinkDistances(population, network, act -> !index.contains(act.getCoord()));

		double mean3 = dist.doubleStream().sum() / dist.size();
		double std3 = Math.sqrt(dist.doubleStream().map(x -> (x - mean3) * (x - mean3)).sum() / dist.size());

		log.info("Distance to nearest link distribution outside shape after adjusting has mean: {}, std: {}", mean3, std3);

		PopulationUtils.writePopulation(population, output.toString());

		return 0;
	}

	/**
	 * Compute distances to the nearest link for all activities that match the predicate.
	 */
	private DoubleList computeLinkDistances(Population population, Network network, Predicate<Activity> f) {
		DoubleList dist = new DoubleArrayList();

		for (Person p : population.getPersons().values()) {

			Plan plan = p.getSelectedPlan();

			for (Activity act : PopulationUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {
				if (f.test(act)) {

					Link link = NetworkUtils.getNearestLinkExactly(network, act.getCoord());

					Coord coord = NetworkUtils.findNearestPointOnLink(act.getCoord(), link);

					dist.add(CoordUtils.calcEuclideanDistance(act.getCoord(), coord));
				}
			}
		}

		return dist;
	}
}
