package org.matsim.modechoice.replanning.scheduled.solver;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.Category;
import org.matsim.modechoice.ModeTargetParameters;

import java.util.Map;

/**
 * Holds all needed information for all mode share target values
 *
 * @param categories categories needed for matching
 * @param params	 maps name to params
 * @param targets    Group name to respective target values.
 * @param mapping    Mapping from person id to the mode choice target.
 */
public record ModeTarget(Map<String, Category> categories,
						 Map<String, ModeTargetParameters> params,
						 Map<String, Object2DoubleMap<String>> targets,
						 Map<Id<Person>, String> mapping) {
}
