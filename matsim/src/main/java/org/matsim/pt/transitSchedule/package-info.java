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
