package org.matsim.core.mobsim.qsim.interfaces;

public interface AgentCounterI {

	public int getLiving();

	public boolean isLiving();

	public int getLost();
	
	// yyyy the following all need to go (no public setters outside the respective framework package).  kai, feb'12

//	public void setLiving(final int count);

//	public void reset();

//	public void incLost(final int count);

//	public void incLiving();

//	public void incLiving(final int count);

//	public void decLiving(final int count);

	public void incLost();

	public void decLiving();

}