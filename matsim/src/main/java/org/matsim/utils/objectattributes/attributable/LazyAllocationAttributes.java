
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
import java.util.function.Supplier;

/**
 * @author cdobler
 */
public final class LazyAllocationAttributes implements Attributes {

	private final Consumer<Attributes> consumer;
	private final Supplier<Attributes> supplier;
	
	public LazyAllocationAttributes(final Consumer<Attributes> consumer, final Supplier<Attributes> supplier) {
		this.consumer = consumer;
		this.supplier = supplier;
	}
	
	@Override
	public Object putAttribute(String attribute, Object value) {
		Attributes attributes = this.supplier.get();
		if (Objects.isNull(attributes)) {
			attributes = new AttributesImpl();
			this.consumer.accept(attributes);
		}
		attributes.putAttribute(attribute, value);
		return null;
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
