package org.matsim.contrib.drt.extension.h3.drtZone;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.LatLng;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * @author nkuehnel / MOIA
 */
public final class H3ZonalSystem {

	static final Logger log = LogManager.getLogger(H3ZonalSystem.class);


	public static DrtZonalSystem createFromPreparedGeometries(Network network,
															  Map<String, PreparedGeometry> geometries,
															  String crs,
															  int resolution) {

		//geometries without links are skipped
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crs, TransformationFactory.WGS84);
		Map<String, List<Link>> linksByGeometryId = StreamEx.of(network.getLinks().values())
			.mapToEntry(l -> getGeometryIdForLink(l, geometries, resolution, ct), l -> l)
			.filterKeys(Objects::nonNull)
			.grouping(toList());

		log.info("Network filtered zone system contains " + linksByGeometryId.size() + " zones for "
			+ network.getLinks().size() + " links and " + network.getNodes().size() + " nodes.");

		//the zonal system contains only zones that have at least one link
		List<DrtZone> zones = EntryStream.of(linksByGeometryId)
			.mapKeyValue((id, links) -> new DrtZone(id, geometries.get(id), links))
			.collect(toList());

		return new DrtZonalSystem(zones);
	}

	/**
	 * @param ct
	 * @param link
	 * @return the the {@code PreparedGeometry} that contains the {@code linkId}.
	 * If a given link's {@code Coord} borders two or more cells, the allocation to a cell is random.
	 * Result may be null in case the given link is outside of the service area.
	 * <p>
	 * Careful: does not work if grid contains different levels of h3 resolutions.
	 */
	@Nullable
	private static String getGeometryIdForLink(Link link, Map<String, PreparedGeometry> geometries, int resolution, CoordinateTransformation ct) {
		H3Core h3 = H3Utils.getInstance();
		LatLng latLng = H3GridUtils.coordToLatLng(ct.transform(link.getToNode().getCoord()));
		String s = h3.latLngToCellAddress(latLng.lat, latLng.lng, resolution);
		if (geometries.containsKey(s)) {
			return s;
		} else {
			return null;
		}
	}
}
