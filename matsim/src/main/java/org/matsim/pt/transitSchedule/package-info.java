
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 /**
 * This package contains classes related to schedules/time tables of public transport (transit).
 * Only the interfaces in .api.* should be used. To createt new elements, use the provided Builder. 
 * 
 * <h2>Package Maintainer(s):</h2>
 * <ul>
 *   <li>Marcel Rieser</li>
 * </ul>
 * 
 *
 * <h2>Structure of a TransitSchedule</h2>
 * <pre>
 * TransitSchedule
 *  |
 *  |- TransitStopFacility (zero or more, 0+)
 *  |   |- id
 *  |   |- coordinate
 *  |   |- link
 *  |   |- isBlockingLane
 *  |
 *  |- TransitLine (0+)
 *      |- lineId
 *      |- TransitRoute (0+)
 *          |- routeId
 *          |- description
 *          |- transportMode
 *          |- TransitRouteStop (0+)
 *          |   |- TransitStopFacility
 *          |   |- arrivalDelay
 *          |   |- departureDelay
 *          |
 *          |- Departure (0+)
 *              |- id
 *              |- departureTime
 *              |- vehicle
 * 
 * </pre>
 * 
 */
package org.matsim.pt.transitSchedule;
