package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import org.matsim.api.core.v01.Identifiable;

import java.util.List;

/**
 * A resource representing multiple {@link RailLink}.
 */
public interface RailResource extends Identifiable<RailResource> {

	/**
	 * Constant returned if no reservation is present.
	 */
	double NO_RESERVATION = -1;

	/**
	 * Type of resource.
	 */
	ResourceType getType();

	/**
	 * The links that are represented by this resource.
	 */
	List<RailLink> getLinks();

	/**
	 * The total capacity of this resource, i.e number of tracks.
	 */
	int getTotalCapacity();

	/**
	 * State of a specific link.
	 */
	ResourceState getState(RailLink link);

}
