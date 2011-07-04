package playground.gregor.sim2d_v2.events;

import java.util.Map;

import org.matsim.core.events.EventImpl;


import com.vividsolutions.jts.geom.Coordinate;

public class DoubleValueStringKeyAtCoordinateEvent extends EventImpl{

	public static final String EVENT_TYPE = "DoubleValueStringKeyAtCoordinate";
	public static final String ATTRIBUTE_CENTER_X = "x";
	public static final String ATTRIBUTE_CENTER_Y = "y";
	public static final String ATTRIBUTE_VALUE = "v";
	public static final String ATTRIBUTE_KEY = "key";

	private final Coordinate center;
	private final double value;
	private final String key;


	public DoubleValueStringKeyAtCoordinateEvent(Coordinate c, double value, String key, double time){
		super(time);
		this.center =c;
		this.value = value;
		this.key = key;
	}


	@Override
	public Map<String, String> getAttributes() {
		Map<String,String> map = super.getAttributes();
		map.put(ATTRIBUTE_CENTER_X, Double.toString(this.center.x));
		map.put(ATTRIBUTE_CENTER_Y, Double.toString(this.center.y));
		map.put(ATTRIBUTE_VALUE, Double.toString(this.value));
		map.put(ATTRIBUTE_KEY, this.key);
		return map;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Coordinate getCoordinate() {
		return this.center;
	}

	public double getValue() {
		return this.value;
	}


	public String getKey() {
		return this.key;
	}

}

