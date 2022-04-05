package org.matsim.freightDemandGeneration;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;

public class FreightDemandGenerationUtils {
	private static final Logger log = Logger.getLogger( CarrierReaderFromCSV.class );
	
	/**
	 * Checks if a link is one of the possible areas.
	 * 
	 * @param link
	 * @param point
	 * @param polygonsInShape
	 * @param possibleAreas
	 * @param crsTransformationNetworkAndShape
	 * @return
	 */
	static boolean checkPositionInShape(Link link, Point point, Collection<SimpleFeature> polygonsInShape,
			String[] possibleAreas, CoordinateTransformation crsTransformationNetworkAndShape) {

		if (polygonsInShape == null)
			return true;
		boolean isInShape = false;
		Point p = null;
		if (link != null && point == null) {
			p = MGC.coord2Point(crsTransformationNetworkAndShape.transform(getCoordOfMiddlePointOfLink(link)));
		} else if (link == null && point != null)
			p = point;
		for (SimpleFeature singlePolygon : polygonsInShape) {
			if (possibleAreas != null) {
				for (String area : possibleAreas) {
					if (area.equals(singlePolygon.getAttribute("Ortsteil"))
							|| area.equals(singlePolygon.getAttribute("BEZNAME")))
						if (((Geometry) singlePolygon.getDefaultGeometry()).contains(p)) {
							isInShape = true;
							return isInShape;
						}
				}
			} else {
				if (((Geometry) singlePolygon.getDefaultGeometry()).contains(p)) {
					isInShape = true;
					return isInShape;
				}
			}
		}
		return isInShape;
	}
	
	/**
	 * Creates the middle coord of a link.
	 * 
	 * @param link
	 * @return Middle coord of the Link
	 */
	static Coord getCoordOfMiddlePointOfLink(Link link) {

		double x, y, xCoordFrom, xCoordTo, yCoordFrom, yCoordTo;
		xCoordFrom = link.getFromNode().getCoord().getX();
		xCoordTo = link.getToNode().getCoord().getX();
		yCoordFrom = link.getFromNode().getCoord().getY();
		yCoordTo = link.getToNode().getCoord().getY();
		if (xCoordFrom > xCoordTo)
			x = xCoordFrom - ((xCoordFrom - xCoordTo) / 2);
		else
			x = xCoordTo - ((xCoordTo - xCoordFrom) / 2);
		if (yCoordFrom > yCoordTo)
			y = yCoordFrom - ((yCoordFrom - yCoordTo) / 2);
		else
			y = yCoordTo - ((yCoordTo - yCoordFrom) / 2);

		return MGC.point2Coord(MGC.xy2Point(x, y));
	}
}
