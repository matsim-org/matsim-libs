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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZEventsFileReader;
import playground.gregor.sim2d_v2.events.XYZEventsHandler;
import playground.gregor.sim2d_v2.events.debug.ArrowEvent;
import playground.gregor.sim2d_v2.events.debug.ArrowEventHandler;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author laemmel
 * 
 */
public class PedVisPeekABot implements XYZEventsHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, ArrowEventHandler {

	private final PeekABotClient pc;
	private String file;
	private final double speedUp;

	private long lastTime = 0;
	double lastEventsTime = 0;
	private String floorShape = null;
	private int segmentCount = 0;
	private static final float FLOOR_HEIGHT = 3.5f;
	double ofX = 0;
	double ofY = 0;

	private final Map<Id,Double> az = new HashMap<Id,Double>();

	public PedVisPeekABot(double speedUp) {
		this.speedUp = speedUp;
		this.pc = new PeekABotClient();
	}

	public void setOffsets(double d, double e) {
		this.ofX =  d;
		this.ofY = e;
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
		this.pc.initII();
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

	public void drawNetwork(Network net) {
		for (Link l : net.getLinks().values()){
			float fromX = (float)(l.getFromNode().getCoord().getX()-this.ofX);
			float fromY = (float)(l.getFromNode().getCoord().getY()-this.ofY);
			float toX = (float)(l.getToNode().getCoord().getX()-this.ofX);
			float toY = (float)(l.getToNode().getCoord().getY()-this.ofY);
			this.pc.drawLink(Integer.parseInt(l.getId().toString()), 0, 0, fromX, fromY, toX, toY);

		}
	}

	private void drawSegment(Point pointN1, Point pointN2) {
		this.pc.initPolygonII(this.segmentCount, 5, .75f, .75f, .75f, 3.f);
		float x1 = (float) (pointN1.getX() - this.ofX);
		float y1 = (float) (pointN1.getY() - this.ofY);
		float x2 = (float) (pointN2.getX() - this.ofX);
		float y2 = (float) (pointN2.getY() - this.ofY);
		this.pc.addPolygonCoordII(this.segmentCount, x1, y1, 0);
		this.pc.addPolygonCoordII(this.segmentCount, x2, y2, 0);
		this.pc.addPolygonCoordII(this.segmentCount, x2, y2, FLOOR_HEIGHT);
		this.pc.addPolygonCoordII(this.segmentCount, x1, y1, FLOOR_HEIGHT);
		this.pc.addPolygonCoordII(this.segmentCount++, x1, y1, 0);

	}

	public PedVisPeekABot(Config c, String events, boolean loop, double speedUp) {
		Module m = c.getModule("sim2d");
		if (m == null) {
			throw new RuntimeException("Module \"sim2d\" is missing in config");
		}
		Sim2DConfigGroup sc = new Sim2DConfigGroup(m);
		this.floorShape = sc.getFloorShapeFile();
		Scenario s = ScenarioUtils.loadScenario(c);
		this.speedUp = speedUp;
		this.pc = new PeekABotClient();
		this.file = events;
		setOffsets(s.getNetwork());
		init();
	}

	private void setOffsets(Network network) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (Node n : network.getNodes().values()) {
			if (n.getCoord().getX() < minX) {
				minX=n.getCoord().getX();
			}
			if (n.getCoord().getY() < minY) {
				minY = n.getCoord().getY();
			}
		}

		this.setOffsets(minX, minY);

	}

	public void setFloorShapeFile(String floorShapeFile) {
		this.floorShape = floorShapeFile;
		init();
	}

	/**
	 * 
	 */
	public void play(boolean loop) {
		if (!loop) {
			play();
		} else {
			while (true) {
				play();
			}
		}

	}

	private void play(){
		EventsManager ev = EventsUtils.createEventsManager();
		ev.addHandler(this);
		XYZEventsFileReader reader = new XYZEventsFileReader(ev);
		try {
			reader.parse(this.file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleEvent(XYZAzimuthEvent e) {
		testWait(e.getTime());
		Double oldAz = this.az.get(e.getPersonId());
		if (oldAz == null) {
			oldAz = 0.;
		}

		this.pc.setBotPositionII(Integer.parseInt(e.getPersonId().toString()), (float) (e.getX() - this.ofX), (float) (e.getY() - this.ofY), (float) e.getZ(), (float) ((oldAz + e.getAzimuth())/2));

		this.az.put(e.getPersonId(), e.getAzimuth());
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
		String config = args[0];
		Config c = ConfigUtils.loadConfig(config);


		PedVisPeekABot vis = new PedVisPeekABot(c,"/Users/laemmel/devel/dfg/output/ITERS/it.0/0.events.xml.gz", true, .5);
		vis.play(true);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		this.pc.removeAllBotsII();
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
		this.pc.addBotII(Integer.parseInt(e.getPersonId().toString()), 10, 10, 10);
		float r = 0;
		float g = 0;
		float b = 0;

		// experimental id dependent colorization
		MatsimRandom.reset(Integer.parseInt(e.getPersonId().toString()));
		MatsimRandom.getRandom().nextDouble();
		MatsimRandom.getRandom().nextDouble();
		b = MatsimRandom.getRandom().nextFloat();
		if (Integer.parseInt(e.getPersonId().toString()) < 240) {
			r = 1.f;
		} else if (Integer.parseInt(e.getPersonId().toString()) == 0) {
			b = 1.f;
		} else {
			g = 1.f;
		}

		this.pc.setBotColorII(Integer.parseInt(e.getPersonId().toString()), r, g, b);
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
		// this.pc.setBotPositionII(Integer.parseInt(e.getPersonId().toString()),
		// -100, -100, -10, -10);
		this.pc.removeBotII(Integer.parseInt(e.getPersonId().toString()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2d.events.debug.ArrowEventHandler#handleEvent(playground
	 * .gregor.sim2d.events.debug.ArrowEvent)
	 */
	@Override
	public void handleEvent(ArrowEvent event) {
		int arrowId = event.getType();
		int agentId = Integer.parseInt(event.getPersId().toString());
		if (agentId != 1) {
			return;
		}
		float r = event.getR();
		float g = event.getG();
		float b = event.getB();
		float fromX = (float) (event.getFrom().x - this.ofX);
		float fromY = (float) (event.getFrom().y - this.ofY);
		float fromZ = 0;//event.getFrom().z;
		float toX = (float) (event.getTo().x - this.ofX);
		float toY = (float) (event.getTo().y - this.ofY);
		float toZ = 0; //event.getTo().z;

		this.pc.drawArrowII(arrowId, agentId, r, g, b, fromX, fromY, fromZ, toX, toY, toZ);
	}
}
