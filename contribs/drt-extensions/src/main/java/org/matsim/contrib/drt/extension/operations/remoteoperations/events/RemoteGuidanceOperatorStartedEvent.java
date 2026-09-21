/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;

import java.util.Map;

/**
 * Fired when a remote guidance operator actually starts its duty period, i.e. when it begins contributing to the
 * supervision coverage capacity and the incident server pool. Together with {@link RemoteGuidanceOperatorEndedEvent}
 * this pair makes the operators' real (runtime) on-duty window observable, so operator utilisation can be reconstructed
 * from events alone rather than by querying mutable runtime state retroactively.
 * <p>
 * Today an operator starts exactly at its planned {@code startTime} (there is no delayed-start mechanism); the event
 * exists so that if a delayed start is ever introduced, the actual start is already the observable quantity.
 *
 * @author nkuehnel / MOIA
 */
public class RemoteGuidanceOperatorStartedEvent extends Event {

	public static final String EVENT_TYPE = "remote guidance operator started";

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_OPERATOR_ID = "operator_id";
	public static final String ATTRIBUTE_CAPACITY = "capacity";

	private final String mode;
	private final Id<DrtShift> operatorId;
	private final int capacity;

	public RemoteGuidanceOperatorStartedEvent(double time, String mode, Id<DrtShift> operatorId, int capacity) {
		super(time);
		this.mode = mode;
		this.operatorId = operatorId;
		this.capacity = capacity;
	}

	public String getMode() {
		return mode;
	}

	public Id<DrtShift> getOperatorId() {
		return operatorId;
	}

	/** The operator's supervision capacity κ (how many vehicles it can supervise simultaneously). */
	public int getCapacity() {
		return capacity;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_OPERATOR_ID, operatorId + "");
		attr.put(ATTRIBUTE_CAPACITY, Integer.toString(capacity));
		return attr;
	}
}
