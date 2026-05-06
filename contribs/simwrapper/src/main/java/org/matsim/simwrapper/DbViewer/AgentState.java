package org.matsim.simwrapper.DbViewer;

import java.util.ArrayList;
import java.util.List;

public class AgentState {

	String agentId;
	double originX, originY;
	String mode;
	double departureTime;
	List<String> linkSequence = new ArrayList<>();

	public void appendLink(String linkId, double time) {
		if (linkSequence.isEmpty()) departureTime = time;
		linkSequence.add(linkId);
	}
}
