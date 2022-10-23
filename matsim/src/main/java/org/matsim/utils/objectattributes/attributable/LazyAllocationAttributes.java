
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
import java.util.function.Consumer;

/**
 * @author cdobler
 */
public final class LazyAllocationAttributes implements Attributes {

	private final Consumer<Attributes> consumer;
	
	public LazyAllocationAttributes(final Consumer<Attributes> consumer) {
		this.consumer = consumer;
	}
	
	@Override
	public Object putAttribute(String attribute, Object value) {
		final Attributes attributes = new AttributesImpl();
		attributes.putAttribute(attribute, value);
		this.consumer.accept(attributes);
		return value;
	}

	@Override
	public Object getAttribute(String attribute) {
		return null;
	}

	@Override
	public Object removeAttribute(String attribute) {
		return null;
	}

	@Override
	public void clear() {
	}

	@Override
	public Map<String, Object> getAsMap() {
		return Collections.emptyMap();
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}
}
