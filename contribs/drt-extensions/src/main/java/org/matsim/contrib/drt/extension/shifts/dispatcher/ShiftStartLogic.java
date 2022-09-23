/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.shifts.dispatcher;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftStartLogic {

	boolean shiftStarts(DrtShiftDispatcher.ShiftEntry shiftEntry);
}
