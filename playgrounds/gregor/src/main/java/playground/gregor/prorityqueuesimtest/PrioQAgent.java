package playground.gregor.prorityqueuesimtest;

import org.matsim.core.mobsim.framework.MobsimDriverAgent;

public class PrioQAgent {

	
	private double linkLeaveTime = -1;
	private final MobsimDriverAgent agent;
	
	public PrioQAgent(MobsimDriverAgent agent) {
		this.agent = agent;
	}
	
	public double getNextLeaveTime() {
		return this.linkLeaveTime;
	}
	
	public void setNextLinkLeaveTime(double time) {
		this.linkLeaveTime = time;
	}
	
	public MobsimDriverAgent getAgent() {
		return this.agent;
	}
}
