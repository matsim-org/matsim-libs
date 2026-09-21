/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 *                                                                         *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

/**
 * Decides whether an active shift should be ended <em>early</em>, i.e. before its scheduled end time. This is a
 * discretionary, per-step decision consulted by the dispatcher, symmetric to {@link ShiftStartLogic}: where
 * {@code ShiftStartLogic} decides when to start an assigned shift, {@code ShiftEndLogic} decides whether to pull a
 * running shift's changeover forward and end it ahead of time.
 * <p>
 * The normal end of a shift is <em>not</em> governed here — a shift's changeover is materialised eagerly at
 * {@code startShift} and the shift ends when the vehicle physically checks in there. This logic only adds the ability
 * to terminate a shift sooner. The {@link #NEVER default} therefore never ends anything early, which is the correct
 * behaviour for mandatory driver shifts (they always run to their scheduled end). Remote guidance binds an
 * implementation that ends virtual shifts early on demand (capacity exceeded, vehicle idle too long, ...).
 *
 * @author nkuehnel / MOIA
 */
public interface ShiftEndLogic {

	/**
	 * @return {@code true} if the given active shift should be ended early now.
	 */
	boolean shiftEndsEarly(DrtShiftDispatcher.ShiftEntry activeShift, double now);

	/**
	 * Default logic for mandatory (driver) shifts: never end early; every shift runs to its scheduled end.
	 */
	ShiftEndLogic NEVER = (activeShift, now) -> false;
}