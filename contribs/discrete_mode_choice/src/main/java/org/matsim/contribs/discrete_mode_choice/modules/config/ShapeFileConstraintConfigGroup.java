package org.matsim.contribs.discrete_mode_choice.modules.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.contribs.discrete_mode_choice.components.constraints.ShapeFileConstraint.Requirement;

/**
 * Config group for the ShapeFileConstraint.
 * 
 * @author sebhoerl
 *
 */
public class ShapeFileConstraintConfigGroup extends ComponentConfigGroup {
	private Requirement requirement = Requirement.BOTH;
	private String path = null;
	private Collection<String> constrainedModes = new HashSet<>();

	public final static String REQUIREMENT = "requirement";
	public final static String PATH = "path";
	public final static String CONSTRAINED_MODES = "constrainedModes";

	public ShapeFileConstraintConfigGroup(String componentType, String componentName) {
		super(componentType, componentName);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();

		String options = Arrays.asList(Requirement.values()).stream().map(String::valueOf)
				.collect(Collectors.joining(", "));
		comments.put(REQUIREMENT,
				"Defines the criterion on when a trip with the constrained mode will be allowed: " + options);
		comments.put(PATH, "Path to a shape file, which should have the same projection as the network.");
		comments.put(CONSTRAINED_MODES, "Modes for which the shapes will be considered.");

		return comments;
	}

	@StringSetter(REQUIREMENT)
	public void setRequirement(Requirement requirement) {
		this.requirement = requirement;
	}

	@StringGetter(REQUIREMENT)
	public Requirement getRequirement() {
		return requirement;
	}

	@StringSetter(PATH)
	public void setPath(String path) {
		this.path = path;
	}

	@StringGetter(PATH)
	public String getPath() {
		return path;
	}

	public void setConstrainedModes(Collection<String> constrainedModes) {
		this.constrainedModes = new HashSet<>(constrainedModes);
	}

	public Collection<String> getConstrainedModes() {
		return constrainedModes;
	}

	@StringSetter(CONSTRAINED_MODES)
	public void setConstrainedModesAsString(String constrainedModes) {
		this.constrainedModes = Arrays.asList(constrainedModes.split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());
	}

	@StringGetter(CONSTRAINED_MODES)
	public String getConstrainedModesAsString() {
		return String.join(", ", constrainedModes);
	}
}
