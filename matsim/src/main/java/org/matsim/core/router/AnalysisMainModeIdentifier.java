/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.router;

/**
 * The purpose of an AnalysisMainModeIdentifier is to identify what is the main mode of a trip for analysis purposes, e.g. mode choice calibartion.
 * E.g. a trip made up of the following legs walk-pt-walk (walk from home to the bus stop, take the bus, walk from the bus stop to work)
 * should be interpreted as a pt trip just as a real world travel survey would count it as a pt trip.
 *
 * This is different from the routing mode {@link RoutingModeMainModeIdentifier} which is only used to assign the correct router for re-routing.
 *
 * If the routing mode was "pt", but no suitable pt service could be found (e.g. if after midnight no service is provided) the actual trip will be
 * a single "walk" leg. For mode choice calibration this trip must be recognized as "walk" and not "pt", because it is de facto "walk"
 * and travel surveys would count it as a "walk" leg.
 *
 * AnalysisMainModeIdentifier is a marker interface for MainModeIdentifier intended to be used in analysis code
 * (e.g. {@link org.matsim.analysis.ModeStatsControlerListener}).
 * It is separate from the deprecated {@link MainModeIdentifier} which is only used for retrofitting old plans without the attribute routing mode.
 *
 * @author vsp-gleich
 */
public interface AnalysisMainModeIdentifier extends MainModeIdentifier {

}
