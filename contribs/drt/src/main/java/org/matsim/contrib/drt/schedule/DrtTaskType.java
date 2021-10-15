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

package org.matsim.contrib.drt.schedule;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import org.matsim.contrib.dvrp.schedule.Task;

import com.google.common.base.MoreObjects;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtTaskType implements Task.TaskType {

	private final String name;

	// can be empty if the task type requires a special handling which is not provided by the standard DRT algorithms
	private final Optional<DrtTaskBaseType> baseType;

	private final int hash;

	public DrtTaskType(@NotNull DrtTaskBaseType baseType) {
		this.name = baseType.name();
		this.baseType = Optional.of(baseType);
		this.hash = Objects.hash(name, baseType);
	}

	public DrtTaskType(String name, @Nullable DrtTaskBaseType baseType) {
		this.name = name;
		this.baseType = Optional.ofNullable(baseType);
		this.hash = Objects.hash(name, baseType);
	}

	@Override
	public final String name() {
		return name;
	}

	public final Optional<DrtTaskBaseType> getBaseType() {
		return baseType;
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DrtTaskType))
			return false;
		DrtTaskType that = (DrtTaskType)o;
		return hash == that.hash && Objects.equals(baseType, that.baseType) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, baseType, hash);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("name", name).add("baseType", baseType).toString();
	}
}
