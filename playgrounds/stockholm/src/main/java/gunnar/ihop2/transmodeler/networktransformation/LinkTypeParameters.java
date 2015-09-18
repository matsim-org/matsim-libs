package gunnar.ihop2.transmodeler.networktransformation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class LinkTypeParameters {

	static final String UNDEFINED = "Undefined";
	static final String FREEWAY = "Freeway";
	static final String RURAL_HIGHWAY = "Rural Highway";
	static final String EXPRESSWAY = "Expressway";
	static final String RAMP = "Ramp";
	static final String SYSTEM_RAMP = "System Ramp";
	static final String MINOR_ARTERIAL = "Minor Arterial";
	static final String MAJOR_ARTERIAL = "Major Arterial";
	static final String MINOR_COLLECTOR = "Minor Collector";
	static final String MAJOR_COLLECTOR = "Major Collector";
	static final String LOCAL_STREET = "Local Street";
	static final String ACCESS_ROAD = "Access Road";
	static final String TRAIL_OR_OTHER_LOCAL_ROAD = "Trail or Other Local Road";
	static final String ROUNDABOUT = "Roundabout";
	static final String TUNNEL = "Tunnel";
	static final String RAIL = "Rail";
	static final String WATERWAY = "Waterway";

	static final String SV70_E = "SV70_E";
	static final String SV70 = "SV70";

	// TODO:
	// SV70_E: capacity 1800 pc/h/l, speed 70 km/h, free flow speed 72 km/h
	// SV70: capacity 2000 pc/h/l, speed 70 km/h, free flow speed 72 km/h

	static final Map<String, LinkTypeParameters> TYPE2PARAMS;

	static {
		final Map<String, LinkTypeParameters> type2params = new LinkedHashMap<String, LinkTypeParameters>();
		type2params.put(UNDEFINED, new LinkTypeParameters(1200, 56));
		type2params.put(FREEWAY, new LinkTypeParameters(2400, 113));
		type2params.put(RURAL_HIGHWAY, new LinkTypeParameters(2000, 97));
		type2params.put(EXPRESSWAY, new LinkTypeParameters(2200, 97));
		type2params.put(RAMP, new LinkTypeParameters(1800, 72));
		type2params.put(SYSTEM_RAMP, new LinkTypeParameters(2000, 80));
		type2params.put(MINOR_ARTERIAL, new LinkTypeParameters(1000, 72));
		type2params.put(MAJOR_ARTERIAL, new LinkTypeParameters(1200, 80));
		type2params.put(MINOR_COLLECTOR, new LinkTypeParameters(800, 56));
		type2params.put(MAJOR_COLLECTOR, new LinkTypeParameters(900, 56));
		type2params.put(LOCAL_STREET, new LinkTypeParameters(600, 48));
		type2params.put(ACCESS_ROAD, new LinkTypeParameters(600, 48));
		type2params.put(TRAIL_OR_OTHER_LOCAL_ROAD, new LinkTypeParameters(500,
				40));
		type2params.put(ROUNDABOUT, new LinkTypeParameters(600, 40));
		type2params.put(TUNNEL, new LinkTypeParameters(1200, 80));
		type2params.put(RAIL, new LinkTypeParameters(600, 121));
		type2params.put(WATERWAY, new LinkTypeParameters(600, 72));
		type2params.put(SV70_E, new LinkTypeParameters(1800, 72));
		type2params.put(SV70, new LinkTypeParameters(2000, 72));

		TYPE2PARAMS = Collections.unmodifiableMap(type2params);
	}

	final double flowCapacity_veh_hLane;

	final double maxSpeed_km_h;

	LinkTypeParameters(final double flowCapacity_veh_hLane,
			final double maxSpeed_km_h) {
		this.flowCapacity_veh_hLane = flowCapacity_veh_hLane;
		this.maxSpeed_km_h = maxSpeed_km_h;
	}

	@Override
	public String toString() {
		return "flowCapacity_veh_hLane=" + this.flowCapacity_veh_hLane
				+ ", maxSpeed_km_h=" + maxSpeed_km_h;
	}

}
