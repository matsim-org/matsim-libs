package org.matsim.ptproject.qsim;

public interface SimTimerI {

	/**
	 * @return Returns the simStartTime. That is the lowest found start time of a leg
	 */
	public double getSimStartTime();

	/**
	 * @return the time of day in seconds
	 */
	public double getTimeOfDay();

	/**
	 * Increments the time by one timestep
	 * @return the new time in seconds
	 */
	public double incrementTime();

	/**
	 * Returns the number of seconds (time steps) the simulation advances when increasing the simulation time.
	 * @return The number of time steps.
	 */
	public double getSimTimestepSize();

	public void setSimStartTime(double startTimeSec);

	public void setTime(double timeSec);

}