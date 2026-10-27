package org.matsim.simwrapper.SelectLinkAnalysis;

import java.util.ArrayList;

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
