/* *********************************************************************** *
 * project: org.matsim.*
 * PedVisPeekABot.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.pedvis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.gregor.sim2d.events.XYZAzimuthEvent;
import playground.gregor.sim2d.events.XYZEventsFileReader;
import playground.gregor.sim2d.events.XYZEventsHandler;
import playground.gregor.sim2d.peekabot.PeekABotClient;

/**
 * @author laemmel
 * 
 */
public class PedVisPeekABot implements XYZEventsHandler, AgentDepartureEventHandler, AgentArrivalEventHandler {

	private final PeekABotClient pc;
	private String file;
	private final double speedUp;

	private long lastTime = 0;
	double lastEventsTime = 0;
	private String floorShape = null;
	private int segmentCount = 0;
	private static final float FLOOR_HEIGHT = 3.5f;
	float ofX = 0;
	float ofY = 0;

	public PedVisPeekABot(double speedUp) {
		this.speedUp = speedUp;
		this.pc = new PeekABotClient();
	}

	/**
	 * 
	 */
	private void init() {
		if (this.floorShape != null) {
			try {
				drawFloor();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.pc.init();
	}

	/**
	 * @throws IOException
	 * 
	 */
	private void drawFloor() throws IOException {
		FeatureSource fs = ShapeFileReader.readDataFile(this.floorShape);
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			MultiPolygon mp = (MultiPolygon) ft.getDefaultGeometry();
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				Geometry geo = mp.getGeometryN(i);
				if (geo instanceof Polygon) {
					drawPolygon((Polygon) geo);
				}

			}

		}
	}

	private void drawPolygon(Polygon p) {
		LineString lr = p.getExteriorRing();

		for (int i = 0; i < lr.getNumPoints() - 1; i++) {
			drawSegment(lr.getPointN(i), lr.getPointN(i + 1));
		}
		for (int i = 0; i < p.getNumInteriorRing(); i++) {
			lr = p.getInteriorRingN(i);
			for (int j = 0; j < lr.getNumPoints() - 1; j++) {
				drawSegment(lr.getPointN(j), lr.getPointN(j + 1));
			}
		}

		// drawSegment(lr.getPointN(lr.getNumPoints()-1), lr.getPointN(0));
	}

	private void drawSegment(Point pointN1, Point pointN2) {
		this.pc.initPolygon(this.segmentCount, 5, .75f, .75f, .75f, 3);
		float x1 = (float) (pointN1.getX() - this.ofX);
		float y1 = (float) (pointN1.getY() - this.ofY);
		float x2 = (float) (pointN2.getX() - this.ofX);
		float y2 = (float) (pointN2.getY() - this.ofY);
		this.pc.addPolygonCoord(this.segmentCount, x1, y1, 0);
		this.pc.addPolygonCoord(this.segmentCount, x2, y2, 0);
		this.pc.addPolygonCoord(this.segmentCount, x2, y2, FLOOR_HEIGHT);
		this.pc.addPolygonCoord(this.segmentCount, x1, y1, FLOOR_HEIGHT);
		this.pc.addPolygonCoord(this.segmentCount++, x1, y1, 0);

	}

	public PedVisPeekABot(String eventsFile, boolean loop, double speedUp) {
		this.speedUp = speedUp;
		this.pc = new PeekABotClient();
		this.pc.init();
		this.file = eventsFile;
		if (!loop) {
			run();
		} else {
			while (true) {
				run();
			}
		}
	}

	public void setFloorShapeFile(String floorShapeFile) {
		this.floorShape = floorShapeFile;
		init();
	}

	/**
	 * 
	 */
	private void run() {
		EventsManagerImpl ev = new EventsManagerImpl();
		ev.addHandler(this);
		XYZEventsFileReader reader = new XYZEventsFileReader(ev);
		try {
			reader.parse(this.file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		;
	}

	public void handleEvent(XYZAzimuthEvent e) {
		testWait(e.getTime());
		this.pc.setBotPosition(Integer.parseInt(e.getPersonId().toString()), (float) e.getX(), (float) e.getY(), (float) e.getZ(), (float) e.getAzimuth());

	}

	/**
	 * @param d
	 * 
	 */
	private void testWait(double d) {
		if (d == this.lastEventsTime) {
			return;
		}
		double step = ((d - this.lastEventsTime) * 1000) / this.speedUp;
		long currentTime = System.currentTimeMillis();
		long diff = (long) (step - (currentTime - this.lastTime));
		if (diff > 0) {
			try {
				Thread.sleep(diff);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.lastEventsTime = d;
		this.lastTime = currentTime;
	}

	public static void main(String[] args) {
		PedVisPeekABot vis = new PedVisPeekABot("/home/laemmel/devel/dfg/output/ITERS/it.0/0.xyzAzimuthEvents.xml.gz", true, 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		this.pc.restAgents();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler
	 * #handleEvent(org.matsim.core.api.experimental.events.AgentDepartureEvent)
	 */
	@Override
	public void handleEvent(AgentDepartureEvent e) {
		this.pc.addBot(Integer.parseInt(e.getPersonId().toString()), 10, 10, 10);
		float r = 0;
		float g = 0;
		float b = 0;

		// experimental id dependent colorization
		MatsimRandom.reset(Integer.parseInt(e.getPersonId().toString()));
		MatsimRandom.getRandom().nextDouble();
		MatsimRandom.getRandom().nextDouble();
		b = MatsimRandom.getRandom().nextFloat();
		if (Integer.parseInt(e.getPersonId().toString()) < 0) {
			r = 1.f;
		} else if (Integer.parseInt(e.getPersonId().toString()) == 0) {
			b = 1.f;
		} else {
			g = 1.f;
		}

		this.pc.setBotColor(Integer.parseInt(e.getPersonId().toString()), r, g, b);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler
	 * #handleEvent(org.matsim.core.api.experimental.events.AgentArrivalEvent)
	 */
	@Override
	public void handleEvent(AgentArrivalEvent e) {
		this.pc.setBotPosition(Integer.parseInt(e.getPersonId().toString()), -100, -100, -10, -10);
		// this.pc.removeBot(Integer.parseInt(e.getPersonId().toString()));
	}

}
