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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.multidestpeds.densityestimation.DensityEstimatorFactory;
import playground.gregor.multidestpeds.densityestimation.NNGaussianKernelEstimator;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEvent;
import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEventHandler;
import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v2.events.debug.ArrowEvent;
import playground.gregor.sim2d_v2.events.debug.ArrowEventHandler;
import playground.gregor.sim2d_v2.helper.GEO;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author laemmel
 * 
 */
public class PedVisPeekABot implements XYVxVyEventsHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, ArrowEventHandler, LinkEnterEventHandler, DoubleValueStringKeyAtCoordinateEventHandler, IterationEndsListener {

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

	double scale = 0.5;

	private static final double TWO_PI = Math.PI * 2;
	private static final double PI_HALF = Math.PI/2;

	//	private final Map<Id,Double> az = new HashMap<Id,Double>();
	private final Map<Id,Coordinate> locations = new HashMap<Id,Coordinate>();
	private final Set<Id> inSim = new HashSet<Id>();

	private final Set<Id> carAgents = new HashSet<Id>();
	private final Scenario sc;

	public PedVisPeekABot(double speedUp, Scenario sc) {
		this.speedUp = speedUp;
		this.sc = sc;
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
			float fromX = (float) ((l.getFromNode().getCoord().getX()-this.ofX) * this.scale);
			float fromY = (float) ((l.getFromNode().getCoord().getY()-this.ofY) * this.scale);
			float toX = (float) ((l.getToNode().getCoord().getX()-this.ofX)  * this.scale);
			float toY = (float) ((l.getToNode().getCoord().getY()-this.ofY) * this.scale);
			this.pc.drawLink(Integer.parseInt(l.getId().toString()), 0, 0, fromX, fromY, toX, toY);

		}
	}

	private void drawSegment(Point pointN1, Point pointN2) {
		this.pc.initPolygonII(this.segmentCount, 5, .75f, .75f, .75f, 3.f);
		float x1 = (float) ((pointN1.getX() - this.ofX)* this.scale);
		float y1 = (float) ((pointN1.getY() - this.ofY)* this.scale);
		float x2 = (float) ((pointN2.getX() - this.ofX)* this.scale);
		float y2 = (float) ((pointN2.getY() - this.ofY)* this.scale);
		this.pc.addPolygonCoordII(this.segmentCount, x1, y1, 0);
		this.pc.addPolygonCoordII(this.segmentCount, x2, y2, 0);
		this.pc.addPolygonCoordII(this.segmentCount, x2, y2, (float) (FLOOR_HEIGHT*this.scale));
		this.pc.addPolygonCoordII(this.segmentCount, x1, y1, (float) (FLOOR_HEIGHT*this.scale));
		this.pc.addPolygonCoordII(this.segmentCount++, x1, y1, 0);

	}

	public PedVisPeekABot(Config c, String events, boolean loop, double speedUp) {
		Module m = c.getModule("sim2d");
		if (m == null) {
			throw new RuntimeException("Module \"sim2d\" is missing in config");
		}
		Sim2DConfigGroup sc = new Sim2DConfigGroup(m);
		c.getModules().put("sim2d", sc);
		this.floorShape = sc.getFloorShapeFile();
		Scenario s = ScenarioUtils.loadScenario(c);
		new ScenarioLoader2DImpl(s).load2DScenario();
		this.sc = s;
		this.speedUp = speedUp;
		this.pc = new PeekABotClient();
		this.file = events;
		setOffsets(s.getNetwork());
		init();
		drawNetwork(s.getNetwork());
	}

	public void setOffsets(Network network) {
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
				reset(1);
			}
		}

	}

	private void play(){
		EventsManager ev = EventsUtils.createEventsManager();
		ev.addHandler(this);
		NNGaussianKernelEstimator est = new DensityEstimatorFactory(ev, this.sc,0.25).createDensityEstimator();
		ev.addHandler(est);
		XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(ev);
		try {
			reader.parse(this.file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleEvent(XYVxVyEvent e) {
		testWait(e.getTime());
		//		System.out.println(e.getPersonId().toString().hashCode());
		this.pc.setBotPositionII(e.getPersonId().toString().hashCode(), (float) ((e.getX() - this.ofX)* this.scale), (float) ((e.getY() - this.ofY)* this.scale), (float) (0* this.scale), (float) (GEO.getAzimuth(e.getVX(),e.getVY())),(float)this.scale);

		this.locations.put(e.getPersonId(), e.getCoordinate());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (!this.carAgents.contains(event.getPersonId())) {
			return;
		}
		testWait(event.getTime());
		Link l = this.sc.getNetwork().getLinks().get(event.getLinkId());
		double x = l.getFromNode().getCoord().getX();
		double y = l.getFromNode().getCoord().getY();
		double az = getAzimuth(l.getFromNode().getCoord(), l.getToNode().getCoord());
		this.pc.setBotPositionII(event.getPersonId().toString().hashCode(), (float) ((x - this.ofX)* this.scale), (float) ((y - this.ofY)* this.scale), 0, (float) az,(float)(this.scale));
		this.locations.put(event.getPersonId(), new Coordinate((float) ((x - this.ofX)* this.scale), (float) ((y - this.ofY)* this.scale), 0));
	}

	/**
	 * @param newPos
	 * @param oldPos
	 * @return
	 */
	private double getAzimuth(Coord oldPos, Coord newPos) {
		double alpha = 0.0;
		double dX = oldPos.getX() - newPos.getX();
		double dY = oldPos.getY() - newPos.getY();
		if (dX > 0) {
			alpha = Math.atan(dY / dX);
		} else if (dX < 0) {
			alpha = Math.PI + Math.atan(dY / dX);
		} else { // i.e. DX==0
			if (dY > 0) {
				alpha = PI_HALF;
			} else {
				alpha = -PI_HALF;
			}
		}
		if (alpha < 0.0)
			alpha += TWO_PI;
		return alpha;
	}

	/**
	 * @param d
	 * 
	 */
	private void testWait(double d) {
		if (d == this.lastEventsTime) {
			return;
		}
		this.pc.endFrame();
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


		PedVisPeekABot vis = new PedVisPeekABot(c,"/Users/laemmel/devel/dfg/events.xml", true, 1.);
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
		this.inSim.clear();
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

		if (this.inSim.contains(e.getPersonId())) {
			Coordinate c = this.locations.get(e.getPersonId());
			this.pc.setBotPositionII(e.getPersonId().toString().hashCode(),(float) c.x, (float)c.y,0,0,(float)this.scale);
		} else {


			Link l = this.sc.getNetwork().getLinks().get(e.getLinkId());
			Coord cc = l.getToNode().getCoord();
			float x = (float)((cc.getX()-this.ofX)*this.scale);
			float y = (float)((cc.getY()-this.ofY)*this.scale);

			int layer =1;
			if (e.getPersonId().toString().contains("ghost")){
				layer = 8;
			}
			this.pc.addBotII(e.getPersonId().toString().hashCode(), x, y, 0, (float)this.scale,layer);
			this.inSim.add(e.getPersonId());

		}
		if (e.getLegMode().equals("car")) {
			this.carAgents.add(e.getPersonId());
		}
		//			this.pc.setBotShapeII(Integer.parseInt(e.getPersonId().toString()), PeekABotClient.CAR);
		//		} else {
		//			this.pc.setBotShapeII(Integer.parseInt(e.getPersonId().toString()), PeekABotClient.WALK_2_D);
		//		}
		float r = 0;
		float g = 0;
		float b = 0;

		// experimental id dependent colorization
		MatsimRandom.reset(e.getPersonId().toString().hashCode());
		MatsimRandom.getRandom().nextDouble();
		MatsimRandom.getRandom().nextDouble();
		b = MatsimRandom.getRandom().nextFloat();
		if (e.getPersonId().toString().contains("r")) {
			r = 1.f;
		} else {
			g = 1.f;
		}
		//
		//		if (Integer.parseInt(e.getPersonId().toString()) == 1) {
		//			b = 1.f;
		//			r=0;
		//			g=0;
		//		}
		//		if (Integer.parseInt(e.getPersonId().toString()) == 500) {
		//			b = 1.f;
		//			r=0.5f;
		//			g=0.5f;
		//		}



		this.pc.setBotColorII(e.getPersonId().toString().hashCode(), r, g, b);
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
		this.carAgents.remove(e.getPersonId());
		this.pc.removeBotII(e.getPersonId().toString().hashCode());
		// this.pc.setBotPositionII(Integer.parseInt(e.getPersonId().toString()),
		// -100, -100, -10, -10);
		//		this.pc.removeBotII(Integer.parseInt(e.getPersonId().toString()));
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
		float fromX = (float) ((event.getFrom().x - this.ofX)* this.scale);
		float fromY = (float) ((event.getFrom().y - this.ofY)* this.scale);
		float fromZ = 0;//event.getFrom().z;
		float toX = (float) ((event.getTo().x - this.ofX)* this.scale);
		float toY = (float) ((event.getTo().y - this.ofY)* this.scale);
		float toZ = 0; //event.getTo().z;

		this.pc.drawArrowII(arrowId, agentId, r, g, b, fromX, fromY, fromZ, toX, toY, toZ);
	}

	@Override
	public void handleEvent(DoubleValueStringKeyAtCoordinateEvent e) {
		//		if (e.getValue() < .01) {
		//			return;
		//		}
		float locX = (float) ((e.getCoordinate().x - this.ofX)*this.scale);
		float locY = (float) ((e.getCoordinate().y - this.ofY)*this.scale);
		float value = Math.min(1.f, (float) (e.getValue()));
		//		if (value <= 0.02) {
		//			value = 0.f;
		//		}
		String key = e.getKey();
		if (key.contains("r")) {
			this.pc.updateOccupancyCell(0,locX,locY,value);
		}
		if (key.contains("g"))  {
			this.pc.updateOccupancyCell(1,locX,locY,value);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		//			this.pc.shutdown();
		//			init();
		//
	}


}
