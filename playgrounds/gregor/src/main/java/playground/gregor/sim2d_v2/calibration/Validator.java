package playground.gregor.sim2d_v2.calibration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentDepartureEventImpl;

import com.vividsolutions.jts.geom.Coordinate;


import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZAzimuthEventImpl;
import playground.gregor.sim2d_v2.events.XYZEventsHandler;
import playground.gregor.sim2d_v2.simulation.Agent2D;
import playground.gregor.sim2d_v2.simulation.Sim2D;

public class Validator implements LinkEnterEventHandler, LinkLeaveEventHandler, XYZEventsHandler, AgentDepartureEventHandler, AgentArrivalEventHandler{

	private static final double correctionTime = 4;

	private double lastCorrection = 0;

	private final LinkedList<XYZAzimuthEvent> actualEvents = new LinkedList<XYZAzimuthEvent>();
	private final LinkedList<XYZAzimuthEvent> desiredEvents = new LinkedList<XYZAzimuthEvent>();

	private LinkedList<Event> calibrationAgentEvents;

	private Id caliId = new IdImpl("1");

	private Sim2D sim;

	private Agent2D calibrationAgent;


	XYZAzimuthEvent last = null;
	XYZAzimuthEvent nextToLast = null;

	private double allDiff = 0;

	public void setCalibrationAgentEvents(
			LinkedList<Event> calibrationAgentEvents) {
		this.calibrationAgentEvents = calibrationAgentEvents;
		this.lastCorrection = 0;
		this.actualEvents.clear();
		this.desiredEvents.clear();

	}

	public Coordinate validate(double time) {
		readDesiredEvents(time);
		if (time >= this.lastCorrection + correctionTime){
			validateAndCorrect(time);
			if (this.last == null) {
				return null;
			}
			this.lastCorrection = time;

			double vx = 0;
			double vy = 0;
			if (this.nextToLast != null) {
				double deltaT = this.last.getTime() - this.nextToLast.getTime();
				double deltaX = this.last.getX() - this.nextToLast.getX();
				double deltaY = this.last.getY() - this.nextToLast.getY();
				vx = deltaX/deltaT;
				vy = deltaY/deltaT;
			}

			this.calibrationAgent.getForce().setVx(vx);
			this.calibrationAgent.getForce().setVy(vy);
			this.calibrationAgent.setCurrentVelocity(vx, vy);
			Coordinate old = (Coordinate) this.calibrationAgent.getPosition().clone();
			this.calibrationAgent.moveToPostion(this.last.getCoordinate());

			System.err.println("moved agent " + this.last.getCoordinate().distance(old));
			this.last = null;
			this.nextToLast = null;
			return old;
		}
		return null;
	}

	private void validateAndCorrect(double time) {

		if (this.actualEvents.size() > this.desiredEvents.size()) {
			compare(this.desiredEvents,this.actualEvents);
		} else {
			compare(this.actualEvents,this.desiredEvents);
		}

		//		for (int i = 0; i < this.actualEvents.size(); i ++) {
		//			XYZAzimuthEvent e1 = this.actualEvents.get(i);
		//			XYZAzimuthEvent e2 = this.desiredEvents.get(i);
		//			double diff = e1.getCoordinate().distance(e2.getCoordinate());
		//			System.err.println("dist:" + diff);
		//		}
	}

	private void compare(LinkedList<XYZAzimuthEvent> e1,
			LinkedList<XYZAzimuthEvent> e2) {
		while (e1.size() > 0) {
			XYZAzimuthEvent xyza1 = e1.poll();
			XYZAzimuthEvent xyza2 = e2.poll();


			if (xyza1.getTime() != xyza2.getTime()) {
				throw new RuntimeException("validator out of sync!\nThis should not happen!");
			}
			double diff = xyza1.getCoordinate().distance(xyza2.getCoordinate());
			this.allDiff  += diff;
			//			System.err.println("dist:" + diff);
		}

	}

	private void readDesiredEvents(double time) {
		while (this.calibrationAgentEvents.size() > 0 && this.calibrationAgentEvents.peek().getTime() <= time){
			Event e = this.calibrationAgentEvents.poll();

			//DEBUG
			if (e instanceof AgentDepartureEvent) {
				AgentDepartureEvent a = (AgentDepartureEvent) e;
				AgentDepartureEventImpl ai = new AgentDepartureEventImpl(a.getTime(), new IdImpl("500"), a.getLinkId(), a.getLegMode());
				this.sim.getEventsManager().processEvent(ai);
			}

			if (e instanceof XYZAzimuthEvent) {
				XYZAzimuthEvent xyza = (XYZAzimuthEvent) e;
				this.desiredEvents.add(xyza);
				this.nextToLast = this.last;
				this.last = xyza;


				//DEBUG
				XYZAzimuthEventImpl xyzan = new XYZAzimuthEventImpl(new IdImpl("500"), xyza.getCoordinate(), xyza.getAzimuth(), xyza.getTime());
				this.sim.getEventsManager().processEvent(xyzan);

			}
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
	public void handleEvent(AgentArrivalEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		// TODO Auto-generated method stub

	}


	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYZAzimuthEvent event) {
		if (event.getPersonId().equals(this.caliId)) {
			this.actualEvents.add(event);
		}
	}

	public void setCalibrationAgent(Agent2D agent) {
		this.calibrationAgent = agent;
	}

	//DEBUG
	public void setSim2D(Sim2D sim) {
		this.sim = sim;
	}

	//DEBUG
	public void setCalibrationAgentId(IdImpl idImpl) {
		this.caliId = idImpl;

	}

}
