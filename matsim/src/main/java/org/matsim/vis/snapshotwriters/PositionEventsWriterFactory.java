package org.matsim.vis.snapshotwriters;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.core.api.experimental.events.EventsManager;

class PositionEventsWriterFactory implements Provider<SnapshotWriter> {

        private final EventsManager eventsManager;

        @Inject
        PositionEventsWriterFactory( EventsManager eventsManager ) {
                this.eventsManager = eventsManager;
        }

        @Override
        public SnapshotWriter get() {
                return new PositionEventsWriter(eventsManager);
        }



}
