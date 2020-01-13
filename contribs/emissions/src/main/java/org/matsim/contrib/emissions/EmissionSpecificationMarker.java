/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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


package org.matsim.contrib.emissions;

/**
 * Created by amit on 29/09/16.
 */
@Deprecated // introduce EmissionsUtils.set/getXxx(...) first, and eventually move to Attributes.  kai, oct'18
public enum EmissionSpecificationMarker {
    
    @Deprecated // introduce EmissionsUtils.set/getXxx(...) first, and eventually move to Attributes.  kai, oct'18
    BEGIN_EMISSIONS ,
    @Deprecated // introduce EmissionsUtils.set/getXxx(...) first, and eventually move to Attributes.  kai, oct'18
    END_EMISSIONS ;
}
