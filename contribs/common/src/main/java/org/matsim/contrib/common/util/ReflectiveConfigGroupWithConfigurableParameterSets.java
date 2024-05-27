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

package org.matsim.contrib.common.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Provides additional functionality for handling parameter sets according to definitions.
 * <p>
 * Handles one-of-many cases where a parameter set has different implementation classes (e.g.
 * taxi/drt config groups contain exactly one optimiser-specific parameter set, but that set can be a subclass of a
 * common parent class)
 *
 * @author Michal Maciejewski (michalm)
 */
public abstract class ReflectiveConfigGroupWithConfigurableParameterSets extends ReflectiveConfigGroup {
	private static class Definition<T extends ConfigGroup> {
		private final Supplier<T> creator;
		private final Supplier<ConfigGroup> getter;
		private final Consumer<ConfigGroup> setter;

		public Definition(Supplier<T> creator, Supplier<ConfigGroup> getter, Consumer<ConfigGroup> setter) {
			this.creator = creator;
			this.getter = getter;
			this.setter = setter;
		}
	}

	private final Map<String, Definition<? extends ConfigGroup>> definitions = new HashMap<>();

	public ReflectiveConfigGroupWithConfigurableParameterSets(String name) {
		super(name);
	}

	protected <T extends ConfigGroup> void addDefinition(String type, Supplier<T> creator, Supplier<ConfigGroup> getter,
			Consumer<ConfigGroup> setter) {
		var oldDefinition = definitions.put(type, new Definition<>(creator, getter, setter));
		Preconditions.checkState(oldDefinition == null, "Parameter set for type (%s) already defined", type);
	}

	@Override
	public final ConfigGroup createParameterSet( String type ) {
		ConfigGroup params = Preconditions.checkNotNull(definition(type).creator.get());
		Verify.verify(params.getName().equals(type), "The created parameter set has type (%s) instead of (%s)",
				params.getName(), type);
		return params;
	}

	@Override
	public final void addParameterSet( ConfigGroup set ) {
		testForLocked() ;
		Definition<?> definition = definition(set.getName());
		ConfigGroup existingParams = definition.getter.get();
		if (existingParams != null) {
			throw new IllegalStateException(
					String.format("Remove the existing parameter set of type (%s) before adding a new one of type (%s)",
							existingParams.getName(), set.getName()));
		}

		definition.setter.accept(set);
		super.addParameterSet(set);
	}

	@Override
	public final boolean removeParameterSet( ConfigGroup set ) {
		Definition<?> definition = definition(set.getName());
		ConfigGroup existingParams = definition.getter.get();
		if (existingParams == null || !existingParams.equals(set)) {
			return false;
		}

		definition.setter.accept(null);
		Verify.verify(super.removeParameterSet(set));
		return true;
	}

	private Definition<?> definition(String type) {
		return Preconditions.checkNotNull(definitions.get(type), "Unsupported parameter set type (%s)", type);
	}
}
