package org.matsim.ptproject.qsim.interfaces;

public interface AgentCounterI {

	public void reset();

	public int getLiving();

	public void setLiving(final int count);

	public boolean isLiving();

	public int getLost();

	public void incLost();

	public void incLost(final int count);

	public void incLiving();

	public void incLiving(final int count);

	public void decLiving();

	public void decLiving(final int count);

}