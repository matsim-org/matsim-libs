package org.matsim.core.basic.network;

import java.util.List;

public interface BasicLaneDefinitions {

	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignments();

	/**
	 * @param assignment
	 */
	public void addLanesToLinkAssignment(BasicLanesToLinkAssignment assignment);

	public BasicLaneDefinitionsBuilder getLaneDefinitionBuilder();

}