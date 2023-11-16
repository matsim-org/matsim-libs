package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

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

}
