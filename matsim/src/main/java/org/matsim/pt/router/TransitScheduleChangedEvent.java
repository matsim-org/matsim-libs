package org.matsim.pt.router;

import org.matsim.api.core.v01.events.Event;

public class TransitScheduleChangedEvent extends Event {
    public TransitScheduleChangedEvent(double time) {
        super(time);
    }

    @Override
    public String getEventType() {
        return "transit_schedule_changed";
    }
}
