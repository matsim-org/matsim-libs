package org.matsim.simwrapper.DbViewer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentState {
	String agentId;
	double originX, originY;
	String mode;
	ArrayList<String> legSequence;
	Integer legIndex;
	Integer tripIndex;

	public String getLegId() {
		return agentId + "_" + legIndex;
	}

	public String getTripId() {
		return agentId + "_" + tripIndex;
	}
}
