package org.matsim.lanes.basic;

import java.util.List;

import org.matsim.core.api.internal.MatsimToplevelContainer;

public interface BasicLaneDefinitions extends MatsimToplevelContainer {

	public List<BasicLanesToLinkAssignment> getLanesToLinkAssignments();

	/**
	 * @param assignment
	 */
	public void addLanesToLinkAssignment(BasicLanesToLinkAssignment assignment);

	public BasicLaneDefinitionsBuilder getBuilder();

}