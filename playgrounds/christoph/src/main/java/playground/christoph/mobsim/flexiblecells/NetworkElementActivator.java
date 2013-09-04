/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkElementActivator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.flexiblecells;

public interface NetworkElementActivator {

	/*package*/ void activateLink(FlexibleCellLink link);

	/*package*/ int getNumberOfSimulatedLinks();
	
	/*package*/ void activateNode(FlexibleCellNode node);

	/*package*/ int getNumberOfSimulatedNodes();
}
