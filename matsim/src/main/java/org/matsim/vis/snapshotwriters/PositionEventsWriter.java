package org.matsim.vis.snapshotwriters;

import org.matsim.core.api.experimental.events.EventsManager;

class PositionEventsWriter implements SnapshotWriter{
        private final EventsManager eventsManager;
        private double now;
        PositionEventsWriter( EventsManager eventsManager ){
                this.eventsManager = eventsManager;
        }
        @Override public void beginSnapshot( double time ){
                now = time;
        }
        @Override public void endSnapshot(){
                //throw new RuntimeException( "not implemented" ); dont do anything
        }
        @Override public void addAgent( AgentSnapshotInfo position ){
                eventsManager.processEvent( new PositionEvent( now, position ) );
        }
        @Override public void finish(){
                //throw new RuntimeException( "not implemented" ); dont do anything
        }
}
