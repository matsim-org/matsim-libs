package org.matsim.core.mobsim.qsim.interfaces;

public interface AgentCounter {

	int getLiving();

	boolean isLiving();

	int getLost();

	void incLost();

	void decLiving();

}