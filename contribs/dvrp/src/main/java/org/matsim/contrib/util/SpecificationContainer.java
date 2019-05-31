/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class SpecificationContainer<I, S extends Identifiable<I>> {
	private final Map<Id<I>, S> specifications = new LinkedHashMap<>();

	public final Map<Id<I>, S> getSpecifications() {
		return Collections.unmodifiableMap(specifications);
	}

	public final void addSpecification(S specification) {
		if (specifications.putIfAbsent(specification.getId(), specification) != null) {
			throw new RuntimeException("A specification with id=" + specification.getId() + " already exists");
		}
	}

	public final void replaceSpecification(S specification) {
		if (specifications.computeIfPresent(specification.getId(), (k, v) -> specification) == null) {
			throw new RuntimeException("A specification with id=" + specification.getId() + " does not exist");
		}
	}

	public final void removeSpecification(Id<I> id) {
		if (specifications.remove(Objects.requireNonNull(id)) == null) {
			throw new RuntimeException("A specification with id=" + id + " does not exist");
		}
	}
}

