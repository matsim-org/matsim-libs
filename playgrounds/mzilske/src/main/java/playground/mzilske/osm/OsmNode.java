/**
 * 
 */
package playground.mzilske.osm;

import java.io.Serializable;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

class OsmNode implements Serializable {
	public final Id id;
	public boolean used = false;
	public int ways = 0;
	public final Coord coord;

	public OsmNode(final Id id, final Coord coord) {
		this.id = id;
		this.coord = coord;
	}
}