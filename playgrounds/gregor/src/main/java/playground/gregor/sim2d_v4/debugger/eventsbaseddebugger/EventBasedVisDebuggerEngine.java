/* *********************************************************************** *
 * project: org.matsim.*
 * EventDecoderEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gregor.sim2d_v4.debugger.eventsbaseddebugger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import playground.gregor.casim.events.CASimAgentConstructEvent;
import playground.gregor.casim.events.CASimAgentConstructEventHandler;
import playground.gregor.casim.simulation.physics.CAMoveableEntity;
import playground.gregor.sim2d_v4.events.*;
import playground.gregor.sim2d_v4.events.debug.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBasedVisDebuggerEngine implements
		CASimAgentConstructEventHandler, XYVxVyEventsHandler,
		 LineEventHandler, ForceReDrawEventHandler,
		RectEventHandler, CircleEventHandler {

	double time;
	private final EventsBasedVisDebugger vis;

	private final Map<Id, CircleProperty> circleProperties = new HashMap<Id, CircleProperty>();
	private final CircleProperty defaultCp = new CircleProperty();

	private final Scenario sc;

	private long lastUpdate = -1;
	private final double dT;
	private final Control keyControl;

	private final List<ClockedVisDebuggerAdditionalDrawer> drawers = new ArrayList<ClockedVisDebuggerAdditionalDrawer>();
	private int nrAgents;
//
//	 FrameSaver fs = new FrameSaver("/Users/laemmel/tmp/processing/nyc/",
//	 "png", 20);

	FrameSaver fs = null;

	public EventBasedVisDebuggerEngine(Scenario sc) {
		this.sc = sc;
		this.dT = 0.1;
		this.vis = new EventsBasedVisDebugger(sc, this.fs);

		this.keyControl = new Control(this.vis.zoomer, 20, this.fs);
		this.vis.addKeyControl(this.keyControl);
		init();
	}

	public void addAdditionalDrawer(VisDebuggerAdditionalDrawer drawer) {
		this.vis.addAdditionalDrawer(drawer);
		if (drawer instanceof ClockedVisDebuggerAdditionalDrawer) {
			this.drawers.add((ClockedVisDebuggerAdditionalDrawer) drawer);
		}
	}

	private void init() {

		this.defaultCp.a = 255;
		this.defaultCp.minScale = 0;
		this.defaultCp.rr = .19f;

		// Links
		LineProperty lp = new LineProperty();
		lp.r = 0;
		lp.g = 0;
		lp.b = 0;
		lp.a = 192;
		lp.minScale = 10;



	}

	@Override
	public void reset(int iteration) {
		this.time = -1;
		this.vis.reset(iteration);

	}

	@Override
	public void handleEvent(XYVxVyEventImpl event) {
		if (event.getTime() > this.time) {
			update(this.time);
			this.time = event.getTime();
		}

		this.nrAgents++;

		double dx = event.getVY();
		double dy = -event.getVX();
		double length = Math.sqrt(dx * dx + dy * dy);
		dx /= length;
		dy /= length;
		double x0 = event.getX() + event.getVX();
		double y0 = event.getY() + event.getVY();
		double al = .20;
		double x1 = x0 + dy * al - dx * al / 4;
		double y1 = y0 - dx * al - dy * al / 4;
		double x2 = x0 + dy * al + dx * al / 4;
		double y2 = y0 - dx * al + dy * al / 4;
		double z = this.vis.zoomer.getZoomScale();
		int a = 255;
		if (z >= 10 && z < 50) {
			z -= 28;
			a = (int) (255. / 32 * z + .5);
		}
		// a=255;
		this.vis.addLine(event.getX(), event.getY(),
				event.getX() + event.getVX() + dy * al,
				event.getY() + event.getVY() - dx * al, 0, 0, 0, a, 15);
		this.vis.addTriangle(x0, y0, x1, y1, x2, y2, 0, 0, 0, a, 15, true);

		CircleProperty cp = this.circleProperties.get(event.getPersonId());
		if (cp == null) {
			cp = new CircleProperty();
			cp.rr = (float) 0.2;//(0.5 / 5.091);
			int nr = event.getPersonId().toString().hashCode() % 100;
			int color = (nr / 10) % 3;
			// if (Integer.parseInt(a.getId().toString()) < 0) {
			// color = 1;
			// } else {
			// color = 2;
			// }
			if (color == 1) {
				cp.r = 255;
				cp.g = 255 - nr;
				cp.b = 0;
				cp.a = 255;
			} else if (color == 2) {
				cp.r = nr - nr;
				cp.g = 0;
				cp.b = 255;
				cp.a = 255;
			} else {
				cp.r = 0;
				cp.g = 255;
				cp.b = 255 - nr;
				cp.a = 255;
			}

			if (event.getPersonId().toString().startsWith("g")) {
				cp.r = 0;
				cp.g = 255 - nr;
				cp.b = 0;
			} else if (event.getPersonId().toString().startsWith("b")) {
				cp.r = 0;
				cp.g = 0;
				cp.b = 255 - nr;
			} else if (event.getPersonId().toString().startsWith("r")) {
				cp.r = 255 - nr;
				cp.g = 0;
				cp.b = 0;
			}

			this.circleProperties.put(event.getPersonId(), cp);
		}

		this.vis.addCircle(event.getX(), event.getY(), cp.rr, cp.r, cp.g, cp.b,
				cp.a, cp.minScale, cp.fill);

			this.vis.addText(event.getX(), event.getY(), event.getPersonId()
					.toString(), 300);
			// this.vis.addText(event.getX(),event.getY(),
			// event.getAgent().toString(), 50);

	}

	@Override
	public void handleEvent(CircleEvent event) {
		// this.vis.addCircle(event.getX(), event.getY(), .18f, 0, 128, 128,
		// 255, 0, true);

	}

	private void update(double time2) {
		this.keyControl.awaitPause();
		this.keyControl.awaitScreenshot();
		this.keyControl.update(time2);
		long timel = System.currentTimeMillis();

		long last = this.lastUpdate;
		long diff = timel - last;
		if (diff < this.dT * 1000 / this.keyControl.getSpeedup()) {
			long wait = (long) (this.dT * 1000 / this.keyControl.getSpeedup() - diff);
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// if (time2 > 1100 && time2 < 1120) {
		// this.keyControl.requestScreenshot();
		// }

		this.vis.update(this.time);
		this.lastUpdate = System.currentTimeMillis();
		for (ClockedVisDebuggerAdditionalDrawer drawer : this.drawers) {
			drawer.update(this.lastUpdate);
			if (drawer instanceof InfoBox) {
				((InfoBox) drawer).setNrAgents(this.nrAgents);
			}
		}
		this.nrAgents = 0;
	}



	private static final class CircleProperty {
		boolean fill = true;
		float rr;
		int r, g, b, a, minScale = 0;
	}

	private static final class LineProperty {
		int r, g, b, a, minScale = 0;
	}

	@Override
	public void handleEvent(CASimAgentConstructEvent e) {
		CAMoveableEntity a = e.getCAAgent();
		CircleProperty cp = new CircleProperty();
		cp.rr = (float) (0.5 / 5.091);
		int nr = a.getId().toString().hashCode() % 100;
		int color = (nr / 10) % 3;
		// if (Integer.parseInt(a.getId().toString()) < 0) {
		// color = 1;
		// } else {
		// color = 2;
		// }
		if (color == 1) {
			cp.r = 255;
			cp.g = 255 - nr;
			cp.b = 0;
			cp.a = 255;
		} else if (color == 2) {
			cp.r = nr - nr;
			cp.g = 0;
			cp.b = 255;
			cp.a = 255;
		} else {
			cp.r = 0;
			cp.g = 255;
			cp.b = 255 - nr;
			cp.a = 255;
		}

		if (a.getId().toString().startsWith("g")) {
			cp.r = 0;
			cp.g = 255 - nr;
			cp.b = 0;
		} else if (a.getId().toString().startsWith("b")) {
			cp.r = 0;
			cp.g = 0;
			cp.b = 255 - nr;
		} else if (a.getId().toString().startsWith("r")) {
			cp.r = 255 - nr;
			cp.g = 0;
			cp.b = 0;
		}

		this.circleProperties.put(a.getId(), cp);

	}
	

	@Override
	public void handleEvent(LineEvent e) {

		if (e.isStatic()) {
			if (e.getGap() == 0) {
				this.vis.addLineStatic(e.getSegment().x0, e.getSegment().y0,
						e.getSegment().x1, e.getSegment().y1, e.getR(),
						e.getG(), e.getB(), e.getA(), e.getMinScale());
			} else {
				this.vis.addDashedLineStatic(e.getSegment().x0,
						e.getSegment().y0, e.getSegment().x1,
						e.getSegment().y1, e.getR(), e.getG(), e.getB(),
						e.getA(), e.getMinScale(), e.getDash(), e.getGap());

			}
		} else {
			if (e.getGap() == 0) {
				this.vis.addLine(e.getSegment().x0, e.getSegment().y0,
						e.getSegment().x1, e.getSegment().y1, e.getR(),
						e.getG(), e.getB(), e.getA(), e.getMinScale());
			} else {
				this.vis.addDashedLine(e.getSegment().x0, e.getSegment().y0,
						e.getSegment().x1, e.getSegment().y1, e.getR(),
						e.getG(), e.getB(), e.getA(), e.getMinScale(),
						e.getDash(), e.getGap());

			}
		}

	}

	@Override
	public void handleEvent(ForceReDrawEvent event) {
		this.keyControl.requestScreenshot();
		update(event.getTime());

	}

	@Override
	public void handleEvent(RectEvent e) {
		this.vis.addRect(e.getTx(), e.getTy(), e.getSx(), e.getSy(), 255, 255,
				255, 255, 0, e.getFill());

	}

	public int getNrAgents() {
		return this.nrAgents;
	}



}
