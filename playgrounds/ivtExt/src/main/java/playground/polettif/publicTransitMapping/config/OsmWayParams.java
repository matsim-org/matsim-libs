package playground.polettif.publicTransitMapping.config;

import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Defines link attributes for converting OSM highway paths
 * into MATSim links.
 *
 * @author polettif
 */
public class OsmWayParams extends ReflectiveConfigGroup implements MatsimParameters {

	public final static String SET_NAME = "wayParams";

	private String osmKey;
	private String osmValue;

	/** number of lanes on that road type **/
	private double lanes;
	/** free speed vehicles can drive on that road type [meters/second] **/
	private double freespeed;
	/** factor the freespeed is scaled **/
	private double freespeedFactor;
	/** capacity per lane [veh/h] **/
	private double laneCapacity;
	/** true to say that this road is a oneway road  **/
	private boolean oneway;

	/**
	 * Constructors
	 */
	public OsmWayParams() {
		super(SET_NAME);
	}

	public OsmWayParams(String osmValue, String osmKey, double lanes, double freespeed, double freespeedFactor, double laneCapacity, boolean oneway) {
		super(SET_NAME);
		this.osmKey = osmKey;
		this.osmValue = osmValue;
		this.lanes = lanes;
		this.freespeed = freespeed;
		this.freespeedFactor = freespeedFactor;
		this.laneCapacity = laneCapacity;
		this.oneway = oneway;
	}

	@StringGetter("osmValue")
	public String getOsmValue() {
		return osmValue;
	}

	@StringSetter("osmValue")
	public void setOsmValue(String osmValue) {
		this.osmValue = osmValue;
	}

	@StringGetter("osmKey")
	public String getOsmKey() {
		return osmKey;
	}

	@StringSetter("osmKey")
	public void setOsmKey(String osmKey) {
		this.osmKey = osmKey;
	}


	@StringGetter("lanes")
	public double getLanes() {
		return lanes;
	}

	@StringSetter("lanes")
	public void setLanes(double lanes) {
		this.lanes = lanes;
	}

	@StringGetter("freespeed")
	public double getFreespeed() {
		return freespeed;
	}

	@StringSetter("freespeed")
	public void setFreespeed(double freespeed) {
		this.freespeed = freespeed;
	}

	@StringGetter("freespeedFactor")
	public double getFreespeedFactor() {
		return freespeedFactor;
	}

	@StringSetter("freespeedFactor")
	public void setFreespeedFactor(double freespeedFactor) {
		this.freespeedFactor = freespeedFactor;
	}

	@StringGetter("laneCapacity")
	public double getLaneCapacity() {
		return laneCapacity;
	}

	@StringSetter("laneCapacity")
	public void setLaneCapacity(double laneCapacity) {
		this.laneCapacity = laneCapacity;
	}

	@StringGetter("oneway")
	public boolean getOneway() {
		return oneway;
	}

	@StringSetter("oneway")
	public void setOneway(boolean oneway) {
		this.oneway = oneway;
	}
}
