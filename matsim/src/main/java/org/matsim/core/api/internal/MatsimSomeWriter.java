/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.api.internal;

/**Marker interface that attempts to mark <i>all</i> matsim writers, including those that do not use the syntax defined by 
 * MatsimWriter.  It attempts to refer to the user interface only, which is why it does not mark the handlers, and for that
 * reason is not marking AbstractMatsimWriter.
 * 
 * @author nagel
 */
public interface MatsimSomeWriter {

}
