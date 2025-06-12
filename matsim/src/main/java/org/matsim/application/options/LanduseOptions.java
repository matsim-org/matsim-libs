package org.matsim.application.options;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import picocli.CommandLine;

import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Options to work with landuse data.
 */
public class LanduseOptions {

	private static final Logger log = LogManager.getLogger(LanduseOptions.class);

	@CommandLine.Option(names = "--landuse", description = "Optional path to shape-file to distribute coordinates according to landuse", required = false)
	private Path landuse;

	@CommandLine.Option(names = "--landuse-iters", description = "Maximum number of points to generate trying to fit into landuse", defaultValue = "500")
	private int iters;

	@CommandLine.Option(names = "--landuse-filter", arity = "0..*", description = "Filter landuse features by certain category", required = false)
	private Set<String> filter;

	@CommandLine.Option(names = "--landuse-filter-attr", description = "Attribute name for the filter", defaultValue = "fclass")
	private String attr;

	/**
	 * Holds the index of geometries
	 */
	private ShpOptions.Index index;

	/**
	 * Create an index of landuse shapes.
	 */
	@Nullable
	public synchronized ShpOptions.Index getIndex(String queryCRS) {

		if (index != null || landuse == null)
			return index;

		log.info("Using landuse from {}, with filter {}", landuse, filter);


		ShpOptions landShp = new ShpOptions(landuse, null, StandardCharsets.UTF_8);

		index = landShp.createIndex(queryCRS, attr, ft-> filter == null || filter.contains(Objects.toString(ft.getAttribute(attr))));

		log.info("Read {} features for landuse", index.size());

		return index;
	}

	/**
	 * Tries to select a point that lies within one of the geometries of the index.
	 * Will try at least {@link #iters} times, after which the last point is returned even if not within a geometry.
	 * When no landuse is configured the first point is returned.
	 *
	 * @param queryCRS crs of the generated coordinates
	 * @param genCoord function to generate a new point
	 */
	public Coord select(String queryCRS, Supplier<Coord> genCoord) {

		ShpOptions.Index index = getIndex(queryCRS);

		Coord coord;
		int i = 0;
		do {
			coord = genCoord.get();
			i++;

			if (index != null) {
				// if the point is in any of the landuse shapes we keep it
				if (index.contains(coord))
					break;
			}

			// regenerate points if there is an index
		} while (i <= iters && index != null);


		return coord;
	}
}
