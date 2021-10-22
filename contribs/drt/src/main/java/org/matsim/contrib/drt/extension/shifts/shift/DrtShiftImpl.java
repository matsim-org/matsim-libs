package org.matsim.contrib.drt.extension.shifts.shift;

import org.matsim.api.core.v01.Id;

/**
 * @author nkuehnel, fzwick
 */
public class DrtShiftImpl implements DrtShift {

	private final Id<DrtShift> id;

	private double start;
	private double end;

	private ShiftBreak shiftBreak;

	private boolean started = false;
	private boolean ended = false;


	public DrtShiftImpl(Id<DrtShift> id) {
		this.id = id;
	}

	@Override
	public void setStartTime(double time) {
		if ((time % 1) != 0) {
			throw new RuntimeException("Cannot use fractions of seconds!");
		}
		this.start = time;
	}

	@Override
	public void setEndTime(double time) {
		if ((time % 1) != 0) {
			throw new RuntimeException("Cannot use fractions of seconds!");
		}
		this.end = time;
	}

	@Override
	public void setBreak(ShiftBreak shiftBreak) {
		this.shiftBreak = shiftBreak;
	}

	@Override
	public double getStartTime() {
		return start;
	}

	@Override
	public double getEndTime() {
		return end;
	}

	@Override
	public ShiftBreak getBreak() {
		return shiftBreak;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public boolean isEnded() {
		return ended;
	}

	@Override
	public void start() {
		if(!started) {
			started = true;
		} else {
			throw new IllegalStateException("Shift already started!");
		}
	}

	@Override
	public void end() {
		if(!ended) {
			ended = true;
		} else {
			throw new IllegalStateException("Shift already ended!");
		}
	}

	@Override
	public void reset() {
		ended = false;
		started = false;
		if(shiftBreak != null) {
			shiftBreak.reset();
		}
	}

	@Override
	public Id<DrtShift> getId() {
		return id;
	}

	@Override
	public String toString() {
		return "Shift " + id.toString() + " ["+start+"-"+end+"]";
	}
}
