package org.matsim.dsim.simulation;

import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;

public class DummyAgentCounter implements AgentCounter {
	@Override
	public int getLiving() {
		return 0;
	}

	@Override
	public int getLost() {
		return 0;
	}

	@Override
	public boolean isLiving() {
		return true;
	}

	@Override
	public void incLost() {

	}

	@Override
	public void decLiving() {

	}
}
