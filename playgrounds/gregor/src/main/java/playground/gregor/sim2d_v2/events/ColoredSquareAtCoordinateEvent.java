package playground.gregor.sim2d_v2.events;

import java.util.Map;

import org.matsim.core.events.EventImpl;


import com.vividsolutions.jts.geom.Coordinate;

public class ColoredSquareAtCoordinateEvent extends EventImpl{

	public static final String EVENT_TYPE = "ColoredSquareAtCoordinateEvent";
	public static final String ATTRIBUTE_CENTER_X = "x";
	public static final String ATTRIBUTE_CENTER_Y = "y";
	public static final String ATTRIBUTE_COLOR_R = "r";
	public static final String ATTRIBUTE_COLOR_G = "g";
	public static final String ATTRIBUTE_COLOR_B = "b";
	public static final String ATTRIBUTE_SIDE_LENGTH = "length";

	private final Coordinate center;
	private final double length;
	private final int b;
	private final int g;
	private final int r;


	public ColoredSquareAtCoordinateEvent(Coordinate c, int r, int g, int b, double length, double time){
		super(time);
		this.center =c;
		this.r = r;
		this.g = g;
		this.b = b;
		this.length = length;
	}


	@Override
	public Map<String, String> getAttributes() {
		Map<String,String> map = super.getAttributes();
		map.put(ATTRIBUTE_CENTER_X, Double.toString(this.center.x));
		map.put(ATTRIBUTE_CENTER_Y, Double.toString(this.center.y));
		map.put(ATTRIBUTE_COLOR_R, Integer.toString(this.r));
		map.put(ATTRIBUTE_COLOR_G, Integer.toString(this.g));
		map.put(ATTRIBUTE_COLOR_B, Integer.toString(this.b));
		map.put(ATTRIBUTE_SIDE_LENGTH, Double.toString(this.length));
		return map;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Coordinate getCoordinate() {
		return this.center;
	}

	public double getSideLength() {
		return this.length;
	}

	public int getR() {
		return this.r;
	}

	public int getG() {
		return this.g;
	}

	public int getB() {
		return this.b;
	}
}

