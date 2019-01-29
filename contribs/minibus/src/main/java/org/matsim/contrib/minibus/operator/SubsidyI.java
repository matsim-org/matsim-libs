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

package org.matsim.contrib.minibus.operator;

import org.matsim.api.core.v01.Id;

/**
* @author ikaddoura
*/

public interface SubsidyI {
	// yyyyyy Müssten wir nicht im Endergebnis etwas bauen, was der normalen person scoring Logik ähnelt?  Ansonsten läuft es doch nur darauf hinaus, dass wir
	// es alle drei Jahre wieder anfassen?
	
	double getSubsidy(Id<PPlan> id);
	// yy My intuition would be to pass objects rather than IDs. kai, mar'17
	// yyyyyy If you look into how it is called, it passes an ad-hoc ID, relying on a convention.  We should try to avoid that.  kai, mar'17
	
	void computeSubsidy();
	// yy My intuition would be to try to do without this. kai, mar'17
	
}

