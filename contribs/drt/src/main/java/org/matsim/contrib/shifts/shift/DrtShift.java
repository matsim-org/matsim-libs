package org.matsim.contrib.shifts.shift;

import org.matsim.api.core.v01.Identifiable;

/**
 * @author nkuehnel, fzwick
 */
public interface DrtShift extends Identifiable<DrtShift> {

	void setStartTime(double time);

	void setEndTime(double time);

	void setBreak(ShiftBreak shiftBreak);

	double getStartTime();

	double getEndTime();

	ShiftBreak getBreak();

	boolean isStarted();

	boolean isEnded();

	void start();

	void end();

	void reset();
}
