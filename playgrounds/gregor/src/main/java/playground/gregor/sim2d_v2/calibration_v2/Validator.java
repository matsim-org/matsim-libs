package playground.gregor.sim2d_v2.calibration_v2;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;



import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZEventsHandler;

public class Validator implements XYZEventsHandler{


	private final LinkedList<XYZAzimuthEvent> actualEvents = new LinkedList<XYZAzimuthEvent>();
	private final LinkedList<XYZAzimuthEvent> desiredEvents = new LinkedList<XYZAzimuthEvent>();

	XYZAzimuthEvent last = null;
	XYZAzimuthEvent nextToLast = null;

	private double allDiff = 0;

	private final EventsManager em;


	private Id caliId;
	private final double delta = 0.00001;

	public Validator(EventsManager em) {
		this.em = em;
	}

	public void initI(Id id) {
		this.caliId = id;
		this.actualEvents.clear();
		this.desiredEvents.clear();
		this.last = null;
	}

	public void validate(PhantomAgent2D agent) {
		validateAndCorrect();
		if (this.last == null) {
			return;
		}

		double vx = 0;
		double vy = 0;
		if (this.nextToLast != null) {
			double deltaT = this.last.getTime() - this.nextToLast.getTime();
			double deltaX = this.last.getX() - this.nextToLast.getX();
			double deltaY = this.last.getY() - this.nextToLast.getY();
			vx = deltaX/deltaT;
			vy = deltaY/deltaT;
		}

		agent.getForce().setVx(vx);
		agent.getForce().setVy(vy);
		agent.setCurrentVelocity(vx, vy);
		agent.moveToPostion(this.last.getCoordinate());

		this.last = null;
		this.nextToLast = null;

	}

	private void validateAndCorrect() {

		if (this.actualEvents.size() > this.desiredEvents.size()) {
			compare(this.desiredEvents,this.actualEvents);
		} else {
			compare(this.actualEvents,this.desiredEvents);
		}

	}

	private void compare(LinkedList<XYZAzimuthEvent> e1,
			LinkedList<XYZAzimuthEvent> e2) {
		while (e1.size() > 0) {
			XYZAzimuthEvent xyza1 = e1.poll();
			XYZAzimuthEvent xyza2 = e2.poll();


			if (Math.abs(xyza1.getTime() - xyza2.getTime()) > this.delta) {
				throw new RuntimeException("validator out of sync!\nThis should not happen!");
			}
			//			double diff = xyza1.getCoordinate().distance(xyza2.getCoordinate());
			double diff = Math.pow(xyza1.getX()-xyza2.getX(), 2) + Math.pow(xyza1.getY()-xyza2.getY(), 2);
			this.allDiff  += diff;
			//			System.err.println("dist:" + diff);
		}

	}


	public double getAndResetAllDiff() {
		double tmp = this.allDiff;
		this.allDiff = 0;
		return tmp;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}


	@Override
	public void handleEvent(XYZAzimuthEvent event) {
		if (event.getPersonId().equals(this.caliId)) {
			this.actualEvents.add(event);
		}
	}

	public void addDesiredEvent(XYZAzimuthEvent event) {
		this.desiredEvents.add(event);
		this.nextToLast = this.last;
		this.last = event;
	}


}
