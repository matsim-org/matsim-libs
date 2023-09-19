package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.LanduseOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

@CommandLine.Command(
		name = "resolve-grid-coords",
		description = "Distribute activities that have been aggregated to grid coordinates"
)
public class ResolveGridCoordinates implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(ResolveGridCoordinates.class);

	/**
	 * It is important to <b>use a car-network</b> (i.e. a network where car is permitted on each link), as otherwise one probably runs into problems.
	 * In scenarios that use access/egress routing, things might still work, but older scenarios will most likely break (during qsim).
	 */
	@CommandLine.Parameters(paramLabel = "INPUT", arity = "1", description = "Path to population")
	private Path input;

	@CommandLine.Option(names = "--grid-resolution", description = "Original grid resolution in meter")
	private double gridResolution;

	@CommandLine.Option(names = "--network", description = "Match to closest link using given network", required = false)
	private Path networkPath;

	@CommandLine.Mixin
	private CrsOptions crs = new CrsOptions();

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private LanduseOptions landuse = new LanduseOptions();

	@CommandLine.Option(names = "--output", description = "Path for output population", required = true)
	private Path output;

	@Override
	public Integer call() throws Exception {

		Population population = PopulationUtils.readPopulation(input.toString());
		Network network = null;
		Geometry geom = null;

		if (shp.getShapeFile() != null) {
			geom = shp.getGeometry();
			log.info("Using shape file for filtering {}", geom);
		}

		if (networkPath != null) {
			network = NetworkUtils.readNetwork(networkPath.toString());
		}


		SplittableRandom rnd = new SplittableRandom(0);

		for (Person p : population.getPersons().values()) {

			// store the mapped coordinates for each person
			// ensures that the location of one activity does not change for one person
			Map<Coord, Coord> mapping = new HashMap<>();

			for (Plan plan : p.getPlans()) {

				for (PlanElement el : plan.getPlanElements()) {
					if (el instanceof Activity) {

						Activity act = (Activity) el;
						Coord coord = act.getCoord();

						if (geom != null && !geom.contains(MGC.coord2Point(coord)))
							continue;

						Coord newCoord = mapping.getOrDefault(coord, null);
						if (newCoord == null) {
							newCoord = CoordUtils.round(landuse.select(crs.getInputCRS(),
									() -> {
										double x = rnd.nextDouble(-gridResolution / 2, gridResolution / 2);
										double y = rnd.nextDouble(-gridResolution / 2, gridResolution / 2);

										if (coord.hasZ())
											return new Coord(coord.getX() + x, coord.getY() + y, coord.getZ());
										else
											return new Coord(coord.getX() + x, coord.getY() + y);
									}
							));
							mapping.put(coord, newCoord);
						}

						act.setCoord(newCoord);

						if (network != null) {
							Link link = NetworkUtils.getNearestLink(network, act.getCoord());
							if(! link.getAllowedModes().contains(TransportMode.car)){
								throw new IllegalArgumentException("About to set linkId for activity" + act + "for person " + p + "to " + link.getId() + ".\n" +
										"However, car is not permitted on this link.\n" +
										"This might cause problems when running the scenario, especially if access/egress routing is not enabled.\n" +
										"Please provide a car-network here (i.e. a network where car is permitted on every link)");
							}
							if (link != null)
								act.setLinkId(link.getId());
						}

					}
				}
			}
		}

		PopulationUtils.writePopulation(population, output.toString());

		return 0;

	}
}
