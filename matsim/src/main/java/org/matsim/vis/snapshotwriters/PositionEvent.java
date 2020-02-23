package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

class PositionEvent extends Event implements BasicLocation, HasPersonId {
        private final AgentSnapshotInfo position;
        PositionEvent( double now, AgentSnapshotInfo position ){
                super(now);
                this.position = position;
        }
        @Override public String getEventType(){
                return "position";
        }
        @Override public Coord getCoord(){
                return new Coord( position.getEasting(), position.getNorthing() );
        }
        @Override public Id<Person> getPersonId(){
                return position.getId();
        }
}
