/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.schedule;

import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;

/**
 * The dynamic activity a vehicle performs while held for a remote-guidance incident. It simply waits in place until the
 * {@link IncidentHoldTask}'s end time, which is owned by the
 * {@link org.matsim.contrib.drt.extension.operations.remoteoperations.IncidentDispatcher}: open (pushed forward each step) while
 * the incident is still queued for an operator, then fixed to the service end once an operator takes it. Any passengers
 * on board stay on board — an incident hold does no boarding/alighting.
 * <p>
 * Functionally this is a plain idle-until-end wait (like {@code IdleDynActivity}); the only reason for a dedicated class
 * is a distinct {@link #ACTIVITY_TYPE}, so incident holds are identifiable as such in events and visualisers (VIA)
 * instead of being indistinguishable from a passenger {@code DrtBusStop}. Mirrors the dedicated
 * {@code WaitForShiftActivity} / {@code ChangeoverActivity} shift activities.
 *
 * @author nkuehnel / MOIA
 */
public class IncidentHoldActivity extends FirstLastSimStepDynActivity {

	public static final String ACTIVITY_TYPE = "RemoteGuidanceIncidentHold";

	private final IncidentHoldTask holdTask;

	public IncidentHoldActivity(IncidentHoldTask holdTask) {
		super(ACTIVITY_TYPE);
		this.holdTask = holdTask;
	}

	@Override
	protected boolean isLastStep(double now) {
		// the dispatcher owns the end time (dynamic while queued); end the activity once it is reached
		return now >= holdTask.getEndTime();
	}
}
