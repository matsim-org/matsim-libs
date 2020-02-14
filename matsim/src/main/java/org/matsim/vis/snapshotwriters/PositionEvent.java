package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.events.Event;

class PositionEvent extends Event {
        private final AgentSnapshotInfo position;
        PositionEvent( double now, AgentSnapshotInfo position ){
                super(now);
                this.position = position;
        }
        @Override public String getEventType(){
                return "position";
        }
}
