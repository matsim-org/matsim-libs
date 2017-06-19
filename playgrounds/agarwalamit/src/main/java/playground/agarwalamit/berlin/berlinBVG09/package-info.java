/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
 * Created by amit on 09.06.17.
 */

/**
 * <li>Input and output files are corresponding to bvg.run189.10pct </li>
 *
 * <li>generated from network file: repos/shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz </li>
 * <li>                plans file: runs-svn/berlin-bvg09/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.plans.filtered.selected.xml.gz </li>
 *
 * <li> There is no network mode in {@link org.matsim.api.core.v01.events.VehicleEntersTrafficEvent} and {@link org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler},
 *  which is then addded to the events from {@link org.matsim.api.core.v01.events.PersonDepartureEvent}. See {@link playground.agarwalamit.berlin.berlinBVG09.BerlinVehicleEnterTrafficEventGenerator}</li>
 **/
package playground.agarwalamit.berlin.berlinBVG09;