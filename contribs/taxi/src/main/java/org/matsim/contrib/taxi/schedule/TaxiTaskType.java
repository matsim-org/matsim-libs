/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.schedule;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.matsim.contrib.dvrp.schedule.Task;

import com.google.common.base.MoreObjects;

import jakarta.validation.constraints.NotNull;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TaxiTaskType implements Task.TaskType {

	private final String name;

	// can be empty if the task type requires a special handling which is not provided by the standard taxi algorithms
	// (e.g. e-taxi charging task cannot be handled as 'STAY')
	private final Optional<TaxiTaskBaseType> baseType;

	private final int hash;

	protected TaxiTaskType(@NotNull TaxiTaskBaseType baseType) {
		this.name = baseType.name();
		this.baseType = Optional.of(baseType);
		this.hash = Objects.hash(name, baseType);
	}

	public TaxiTaskType(String name, @Nullable TaxiTaskBaseType baseType) {
		this.name = name;
		this.baseType = Optional.ofNullable(baseType);
		this.hash = Objects.hash(name, baseType);
	}

	@Override
	public final String name() {
		return name;
	}

	public final Optional<TaxiTaskBaseType> getBaseType() {
		return baseType;
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TaxiTaskType taskType)) {
			return false;
		}
		return hash == taskType.hash && baseType.equals(taskType.baseType) && Objects.equals(name, taskType.name);
	}

	@Override
	public final int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", name).add("baseType", baseType).toString();
	}
}
