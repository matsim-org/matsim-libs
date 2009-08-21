package org.matsim.lanes.basic;

import java.util.List;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

public interface BasicLaneDefinitions extends MatsimToplevelContainer {

	/**
	 * @deprecated use the map instead 
	 */
	@Deprecated
	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignmentsList();

	/**
	 * 
	 * @return Map with Link Ids as keys and assignments as values
	 */
	public Map<Id, BasicLanesToLinkAssignment> getLanesToLinkAssignments();
	
	/**
	 * @param assignment
	 */
	public void addLanesToLinkAssignment(BasicLanesToLinkAssignment assignment);

	public BasicLaneDefinitionsBuilder getBuilder();

}