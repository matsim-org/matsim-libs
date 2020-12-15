package org.matsim.contrib.emissions.events;

import org.matsim.api.core.v01.events.Event;
import org.matsim.vis.snapshotwriters.PositionEvent;

import java.util.Map;

public class EmissionPositionEvent extends Event {

    private final PositionEvent position;
    private final EmissionEvent emission;

    public EmissionPositionEvent(PositionEvent positionEvent, EmissionEvent emissionEvent) {
        super(positionEvent.getTime());
        this.position = positionEvent;
        this.emission = emissionEvent;
    }

    @Override
    public Map<String, String> getAttributes() {
        var attr = super.getAttributes();
        // there are multiple duplicated keys in both events which should also have the same values
        // the map should sort this out.
        attr.putAll(position.getAttributes());
        attr.putAll(emission.getAttributes());
        return attr;
    }

    @Override
    public String getEventType() {
        return "emissionPositionEvent";
    }
}
