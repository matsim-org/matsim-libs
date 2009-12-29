/* *********************************************************************** *
 * project: org.matsim.*
 * InteractorFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.jhackney.deprecated;

import playground.jhackney.interactions.NonSpatialInteractor;
import playground.jhackney.socialnet.SocialNetwork;
/**
 * Deprecated
 * @author jhackney
 *
 */
public class InteractorFactory {
    public static Interactor createNonSpatialInteractor(String type, SocialNetwork net) {

	Interactor inter;
	if (type.equals("random")) {
	    inter = new NonSpatialInteractor(net);
	}
	else inter = null;
	inter.setType(type);
	return inter;
    }
    public static Interactor createSpatialInteractor(String type, SocialNetwork net) {

	Interactor inter;
	if (type.equals("random")){
	    inter = new SpatialInteractor(net);
	}
	else inter = null;
	inter.setType(type);
	return inter;
    }
}
