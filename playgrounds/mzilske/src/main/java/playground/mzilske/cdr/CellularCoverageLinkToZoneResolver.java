package playground.mzilske.cdr;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import playground.mzilske.cdr.ZoneTracker.LinkToZoneResolver;

import java.util.Random;

public final class CellularCoverageLinkToZoneResolver implements
		LinkToZoneResolver {
	private final Zones cellularCoverage;
	private final Network network;

	public CellularCoverageLinkToZoneResolver(Zones cellularCoverage,
			Network network) {
		this.cellularCoverage = cellularCoverage;
		this.network = network;
	}

	@Override
	public Id resolveLinkToZone(Id<Link> linkId) {
		return cellularCoverage.locate(network.getLinks().get(linkId).getCoord());
	}

	@Override
	public Id<Link> chooseLinkInZone(String zoneId) {
		CellTower cellTower = cellularCoverage.cellTowers.get(zoneId);
		Geometry cell = cellTower.cell;
		Point p = getRandomPointInFeature(MatsimRandom.getRandom(), cell);
		Coord coord = new Coord(p.getX(), p.getY());
		Link link = NetworkUtils.getNearestLink(((NetworkImpl) network), coord);
		return link.getId();
	}
	
	private static Point getRandomPointInFeature(Random rnd, Geometry ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (ft.getEnvelopeInternal().getMaxX() - ft.getEnvelopeInternal().getMinX());
			y = ft.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (ft.getEnvelopeInternal().getMaxY() - ft.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!ft.contains(p));
		return p;
	}
	
}