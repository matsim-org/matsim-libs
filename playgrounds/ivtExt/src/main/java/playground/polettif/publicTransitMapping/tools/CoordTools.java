/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.polettif.publicTransitMapping.tools;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.HashMap;
import java.util.Map;

public class CoordTools {
	/**
	 * @return the azimuth in [rad] of a line defined by two points.
	 */
	public static double getAzimuth(Coord from, Coord to) {
		double deltaE = to.getX()-from.getX();
		double deltaN = to.getY()-from.getY();

		double az2 = Math.atan2(deltaE, deltaN);

		if(az2 < 0)
			az2 = az2+2*Math.PI;

		if(az2 >= 2*Math.PI)
			az2 = az2-2*Math.PI;

		return az2;
	}

	/**
	 * @return Returns the point on the line between lineStart and lineEnd which
	 * is closest to refPoint.
	 */
	public static Coord getClosestPointOnLine(Coord lineStart, Coord lineEnd, Coord refPoint) {
		double azLine = getAzimuth(lineStart, lineEnd);
		double azPoint = getAzimuth(lineStart, refPoint);
		double azDiff = (azLine > azPoint ? azLine-azPoint : azPoint-azLine);

		double distanceToNewPoint = Math.cos(azDiff) * CoordUtils.calcEuclideanDistance(lineStart, refPoint);

		// assuming precision < 1 mm is not needed
		double newN = lineStart.getY() + Math.round(Math.cos(azLine) * distanceToNewPoint * 1000) / 1000.;
		double newE = lineStart.getX() + Math.round(Math.sin(azLine) * distanceToNewPoint * 1000) / 1000.;

		return new Coord(newE, newN);
	}

	/**
	 * Calculates the minimal distance between a stop facility and a link via {@link CoordUtils#distancePointLinesegment}
	 * @param stopFacility
	 * @param link
	 * @return
	 */
	public static double distanceStopFacilityToLink(TransitStopFacility stopFacility, Link link) {
		return CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), stopFacility.getCoord());
	}

	/**
	 * Calculates the extent of the given network.
	 * @return Array of Coords with the minimal South-West and the
	 * 		   maximal North-East Coordinates
	 */
	public static Coord[] getExtent(Network network) {
		double maxE = 0;
		double maxN = 0;
		double minS = Double.MAX_VALUE;
		double minW = Double.MAX_VALUE;

		for(Node node : network.getNodes().values()) {
			if(node.getCoord().getX() > maxE) {
				maxE = node.getCoord().getX();
			}
			if(node.getCoord().getY() > maxN) {
				maxN = node.getCoord().getY();
			}
			if(node.getCoord().getX() < minW) {
				minW = node.getCoord().getX();
			}
			if(node.getCoord().getY() < minS) {
				minS = node.getCoord().getY();
			}
		}

		return new Coord[]{new Coord(minW, minS), new Coord(maxE, maxN)};
	}

	/**
	 * Checks if a coordinate is in the area given by SE and NE.
	 * @param coord the coordinate to check
	 * @param SW the south-west corner of the area
	 * @param NE the north-east corner of the area
	 */
	public static boolean isInArea(Coord coord, Coord SW, Coord NE) {
		return (getCompassQuarter(SW, coord) == 1 && getCompassQuarter(NE, coord) == 3);
	}

	/**
	 * Checks if a coordinate is in the area
	 * @param coord the coordinate to check
	 * @param area [0] the south-west corner of the area, [1] the north-east corner of the area
	 */
	public static boolean isInArea(Coord coord, Coord[] area) {
		return (getCompassQuarter(area[0], coord) == 1 && getCompassQuarter(area[1], coord) == 3);
	}

	/**
	 * @return whether Coord2 lies<br/>
	 * [1] North-East<br/>
	 * [2] South-East<br/>
	 * [3] South-West<br/>
	 * [4] North-West<br/>
	 * of Coord1
	 */
	public static int getCompassQuarter(Coord baseCoord, Coord toCoord) {
		double az = getAzimuth(baseCoord, toCoord);


		if(az < Math.PI/2) {
			return 1;
		} else if(az >= Math.PI/2 && az < Math.PI) {
			return 2;
		} else if(az > Math.PI && az < 1.5*Math.PI) {
			return 3;
		} else {
			return 4;
		}
	}

	/**
	 * @return a Map with a boolean for each stop facility denoting whether
	 * the facility is within the area or not.
	 */
	public static Map<TransitStopFacility, Boolean> getStopsInAreaBool(TransitSchedule schedule, Coord[] area) {
		HashMap<TransitStopFacility, Boolean> stopsInArea = new HashMap<>();
		for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			stopsInArea.put(stopFacility, isInArea(stopFacility.getCoord(), area));
		}
		return stopsInArea;
	}

	/**
	 * @return which border of a rectangular area of interest a line fromCoord-toCoord crosses. One coord has to be
	 * inside area of interest<br/>
	 * [10] north->inside<br/>
	 * [17] inside->north<br/>
	 * [20] east->inside<br/>
	 * [27] inside->east<br/>
	 * [30] south->inside<br/>
	 * [37] inside->south<br/>
	 * [40] west->inside<br/>
	 * [47] inside->west<br/>
	 * [0] line does not cross any border
	 */
	@Deprecated
	public static int getBorderCrossType(Coord SWcut, Coord NEcut, Coord fromCoord, Coord toCoord) {
		int fromSector = getAreaOfInterestSector(SWcut, NEcut, fromCoord);
		int toSector = getAreaOfInterestSector(SWcut, NEcut, toCoord);

		if(fromSector == toSector) {
			return 0;
		}

		double azFromTo = getAzimuth(fromCoord, toCoord);
		double azFromSW = getAzimuth(fromCoord, SWcut);
		double azFromNE = getAzimuth(fromCoord, NEcut);

		double azToFrom = getAzimuth(toCoord, fromCoord);
		double azToSW = getAzimuth(toCoord, SWcut);
		double azToNE = getAzimuth(toCoord, NEcut);

		if(fromSector != 0 ) {
			switch (fromSector) {
				case 1:
					return 10;
				case 2: {
					if(azFromTo > azFromNE)
						return 10;
					else
						return 20;
				}
				case 3:
					return 20;
				case 4: {
					if(azFromTo > azFromNE)
						return 20;
					else
						return 30;
				}
				case 5:
					return 30;
				case 6: {
					if(azFromTo > azFromSW)
						return 30;
					else
						return 40;
				}
				case 7:
					return 40;
				case 8: {
					if(azFromTo > azFromSW)
						return 40;
					else
						return 10;
				}
			}
		}

		if(toSector != 0) {
			switch (toSector) {
				case 1:
					return 17;
				case 2: {
					if(azToFrom < azToNE)
						return 17;
					else
						return 27;
				}
				case 3:
					return 27;
				case 4: {
					if(azToFrom < azToNE)
						return 27;
					else
						return 37;
				}
				case 5:
					return 37;
				case 6: {
					if(azToFrom < azToSW)
						return 37;
					else
						return 47;
				}
				case 7:
					return 47;
				case 8: {
					if(azToFrom< azToSW)
						return 47;
					else
						return 17;
				}
			}
		}

		return 0;
	}

	@Deprecated
	private static int getAreaOfInterestSector(Coord SWcut, Coord NEcut, Coord c) {
		int qSW = getCompassQuarter(SWcut, c);
		int qNE = getCompassQuarter(NEcut, c);

		if(qSW == 1 && qNE == 3) {
			return 0;
		} else if(qSW == 1 && qNE == 4) {
			return 1;
		} else if(qSW == 1 && qNE == 1) {
			return 2;
		} else if(qSW == 1 && qNE == 2) {
			return 3;
		} else if(qSW == 2 && qNE == 2) {
			return 4;
		} else if(qSW == 2 && qNE == 3) {
			return 5;
		} else if(qSW == 3 && qNE == 3) {
			return 6;
		} else if(qSW == 4 && qNE == 3) {
			return 7;
		} else if(qSW == 4 && qNE == 4) {
			return 8;
		}

		return 0;
	}
}
