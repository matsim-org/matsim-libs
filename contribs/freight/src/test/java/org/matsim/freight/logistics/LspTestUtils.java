package org.matsim.freight.logistics;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

public class LspTestUtils {

	public static boolean isWithinBound(Link pendingFromLink, Coord minCoord, Coord maxCoord) {
		Coord fromNodeCoord = pendingFromLink.getFromNode().getCoord();
		Coord toNodeCoord = pendingFromLink.getToNode().getCoord();
		return fromNodeCoord.getX() >= minCoord.getX() &&
			fromNodeCoord.getX() <= maxCoord.getX() &&
			fromNodeCoord.getY() >= minCoord.getY() &&
			fromNodeCoord.getY() <= maxCoord.getY() &&
			toNodeCoord.getX() >= minCoord.getX() &&
			toNodeCoord.getX() <= maxCoord.getX() &&
			toNodeCoord.getY() >= minCoord.getY() &&
			toNodeCoord.getY() <= maxCoord.getY();
	}
}
