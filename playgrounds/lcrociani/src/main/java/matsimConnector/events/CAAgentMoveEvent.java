package matsimConnector.events;

import java.util.Map;

import matsimConnector.agents.Pedestrian;

import org.matsim.api.core.v01.events.Event;

import pedCA.environment.grid.GridPoint;
import pedCA.utility.DirectionUtility.Heading;


public class CAAgentMoveEvent extends Event {
	public static final String EVENT_TYPE = "CAAgentMoveEvent";
	public static final String ATTRIBUTE_REAL_TIME = "real_time";
	public static final String ATTRIBUTE_PERSON = "pedestrian";
	public static final String ATTRIBUTE_FROM_X = "from_x";
	public static final String ATTRIBUTE_FROM_Y = "from_y";
	public static final String ATTRIBUTE_TO_X = "to_x";
	public static final String ATTRIBUTE_TO_Y = "to_y";
	public static final String ATTRIBUTE_DIRECTION = "direction";
	
	private Pedestrian pedestrian;
	private final double realTime;	
	private final int from_x;
	private final int from_y;
	private final int to_x;
	private final int to_y;
	private final Heading direction;
	


	public CAAgentMoveEvent(double time, Pedestrian pedestrian, GridPoint position, GridPoint nextPosition) {
		super((int)time+1);
		this.realTime = time;
		this.pedestrian = pedestrian;
		this.from_x = position.getX();
		this.from_y = position.getY();
		this.to_x = nextPosition.getX();
		this.to_y = nextPosition.getY();
		this.direction = pedestrian.getHeading();
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, pedestrian.getId().toString());
		attr.put(ATTRIBUTE_REAL_TIME, Double.toString(this.realTime));
		attr.put(ATTRIBUTE_FROM_X, Double.toString(this.from_x));
		attr.put(ATTRIBUTE_FROM_Y, Double.toString(this.from_y));
		attr.put(ATTRIBUTE_TO_X, Double.toString(this.to_x));
		attr.put(ATTRIBUTE_TO_Y, Double.toString(this.to_y));
		attr.put(ATTRIBUTE_DIRECTION, direction.toString());
		
		return attr;
	}

	public int getFrom_x() {
		return from_x;
	}

	public int getFrom_y() {
		return from_y;
	}

	public int getTo_x() {
		return to_x;
	}

	public int getTo_y() {
		return to_y;
	}

	public double getRealTime() {
		return realTime;
	}
	
	public Pedestrian getPedestrian() {
		return pedestrian;
	}
	
	public Heading getDirection(){
		return direction;
	}
}
