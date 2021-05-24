package org.matsim.contrib.carsharing.relocation.events;

import org.matsim.api.core.v01.events.Event;

public class DispatchRelocationsEvent extends Event {

	public static final String EVENT_TYPE = "Dispatch free-floating vehicle relocations";

	public double end;

	public String companyId;

	public DispatchRelocationsEvent(double start, double end, String companyId) {
		super(start);

		this.end = end;
		this.companyId = companyId;

	}
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public double getStart() {
		return this.getTime();
	}

	public double getEnd() {
		return this.end;
	}

	public String getCompanyId() {
		return this.companyId;
	}
}
