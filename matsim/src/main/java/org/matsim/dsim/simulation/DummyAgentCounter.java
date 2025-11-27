package org.matsim.dsim.simulation;

import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;

public class DummyAgentCounter implements AgentCounter {

	private int living = 0;
	private int lost = 0;

	@Override
	public int getLiving() {
		return living;
	}

	@Override
	public int getLost() {
		return lost;
	}

	@Override
	public boolean isLiving() {
		return living > 0;
	}

	@Override
	public void incLost() {
		lost++;
	}

	@Override
	public void decLiving() {
		living--;
	}
}
