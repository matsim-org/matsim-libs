package playground.balac.twowaycarsharingredisigned.scenario;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class TwoWayCSFacilityImpl implements TwoWayCSFacility{
	private final Coord coord;
	private final Id<TwoWayCSFacility> id;
	private final Id<Link> linkId;
	private final Map<String, Object> customAttributes = new LinkedHashMap<String, Object>();

	public TwoWayCSFacilityImpl(
			final Id<TwoWayCSFacility> id,
			final Coord coord,
			final Id<Link> linkId) {
		this.id = id;
		this.coord = coord;
		this.linkId = linkId;
		
	}
		public Coord getCoord() {
			return coord;
		}

		public Id<TwoWayCSFacility> getId() {
			return id;
		}

		public Map<String, Object> getCustomAttributes() {
			return customAttributes;
		}

		public Id<Link> getLinkId() {
			return linkId;
		}
}
