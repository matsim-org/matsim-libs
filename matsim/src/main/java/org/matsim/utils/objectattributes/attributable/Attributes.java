
/* *********************************************************************** *
 * project: org.matsim.*
 * Attributes.java
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

 package org.matsim.utils.objectattributes.attributable;

import java.util.*;

/**
 * @author cdobler
 */
public interface Attributes {

	public Object putAttribute( final String attribute, final Object value);

	public Object getAttribute( final String attribute);

	public Object removeAttribute( final String attribute );

	public void clear();

	public Map<String, Object> getAsMap();

	public int size();

	public boolean isEmpty();
}
