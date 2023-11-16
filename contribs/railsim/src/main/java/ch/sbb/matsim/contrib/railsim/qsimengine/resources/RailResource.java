package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import java.util.List;

/**
 * A resource representing multiple {@link RailLink}.
 */
public interface RailResource {

	/**
	 * Type of resource.
	 */
	ResourceType getType();

	/**
	 * The links that are represented by this resource.
	 */
	List<RailLink> getLinks();

	/**
	 * State of a specific link.
	 */
	ResourceState getState(RailLink link);

}
