package org.matsim.application.prepare.population;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.SplittableRandom;

@CommandLine.Command(
		name = "resolve-grid-coords",
		description = "Distribute activities that have been aggregated to grid coordinates"
)
public class ResolveGridCoordinates implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(ResolveGridCoordinates.class);

	@CommandLine.Parameters(paramLabel = "INPUT", arity = "1", description = "Path to population")
	private Path input;

	@CommandLine.Option(names = "--grid-resolution", description = "Original grid resolution in meter")
	private double gridResolution;

	@CommandLine.Option(names = "--landuse", description = "Optional path to shape-file to distribute coordinates according to landuse", required = false)
	private Path landuse;

	@CommandLine.Option(names = "--landuse-iters", description = "Maximum number of points to generate trying to fit into landuse", defaultValue = "250")
	private int iters;

	@CommandLine.Option(names = "--network", description = "Match to closest link using given network", required = false)
	private Path networkPath;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

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

		STRtree index = null;
		if (landuse != null) {

			log.info("Using landuse from {}", landuse);

			ShpOptions landShp = new ShpOptions(landuse, null, StandardCharsets.UTF_8);
			index = new STRtree();

			for (SimpleFeature ft : landShp.readFeatures()) {
				Geometry theGeom = (Geometry) ft.getDefaultGeometry();
				index.insert(theGeom.getEnvelopeInternal(), theGeom);
			}

			index.build();

			log.info("Read {} features for landuse", index.size());
		}

		GeometryFactory f = JTSFactoryFinder.getGeometryFactory();

		SplittableRandom rnd = new SplittableRandom(0);

		for (Person p : population.getPersons().values()) {

			for (Plan plan : p.getPlans()) {

				for (PlanElement el : plan.getPlanElements()) {
					if (el instanceof Activity) {

						Activity act = (Activity) el;
						Coord coord = act.getCoord();

						if (geom != null && !geom.contains(MGC.coord2Point(coord)))
							continue;

						double x, y;
						int i = 0;
						outer:
						do {
							x = rnd.nextDouble(-gridResolution / 2, gridResolution / 2);
							y = rnd.nextDouble(-gridResolution / 2, gridResolution / 2);
							i++;

							if (index != null) {
								Coordinate newCoord = new Coordinate(coord.getX() + x, coord.getY() + y);
								List<Geometry> result = index.query(new Envelope(newCoord));

								// if the point is in any of the landuse shapes we keep it
								for (Geometry r : result) {
									if (r.contains(f.createPoint(newCoord)))
										break outer;
								}
							}

							// regenerate points if there is an index
						} while (i <= iters && index != null);


						if (coord.hasZ())
							act.setCoord(new Coord(coord.getX() + x, coord.getY() + y, coord.getZ()));
						else
							act.setCoord(new Coord(coord.getX() + x, coord.getY() + y));

						if (network != null) {
							Link link = NetworkUtils.getNearestLink(network, act.getCoord());
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
