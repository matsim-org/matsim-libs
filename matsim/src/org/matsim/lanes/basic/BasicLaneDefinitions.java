package org.matsim.lanes.basic;

import java.util.List;

public interface BasicLaneDefinitions {

	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignments();

	/**
	 * @param assignment
	 */
	public void addLanesToLinkAssignment(BasicLanesToLinkAssignment assignment);

	public BasicLaneDefinitionsBuilder getLaneDefinitionBuilder();

}