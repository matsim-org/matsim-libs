package org.matsim.lanes;

import java.util.List;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

/**
 * Top level container for lanes within MATSim. See package-info for documentation.
 * @author dgrether
 *
 */
public interface LaneDefinitions extends MatsimToplevelContainer {

	/**
	 * @deprecated use the map instead 
	 */
	@Deprecated
	public List<LanesToLinkAssignment> getLanesToLinkAssignmentsList();

	/**
	 * 
	 * @return Map with Link Ids as keys and assignments as values
	 */
	public SortedMap<Id, LanesToLinkAssignment> getLanesToLinkAssignments();
	
	/**
	 * Adds a LanesToLinkAssignment to the container.
	 * @param assignment
	 */
	public void addLanesToLinkAssignment(LanesToLinkAssignment assignment);
	/**
	 * Get the factory to create container content. 
	 */
	public LaneDefinitionsFactory getFactory();

}