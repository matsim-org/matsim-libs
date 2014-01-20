package playground.balac.freefloating.scenario;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class FreeFloatingFacilityImpl implements FreeFloatingFacility{
	private final Coord coord;
	private final Id id;
	private final Id linkId;
	private final Map<String, Object> customAttributes = new LinkedHashMap<String, Object>();

	public FreeFloatingFacilityImpl(
			final Id id,
			final Coord coord,
			final Id linkId) {
		this.id = id;
		this.coord = coord;
		this.linkId = linkId;
		
	}
		@Override
		public Coord getCoord() {
			return coord;
		}

		@Override
		public Id getId() {
			return id;
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return customAttributes;
		}

		@Override
		public Id getLinkId() {
			return linkId;
		}

}
