/* *********************************************************************** *
 * project: org.matsim.*
 * PhysicalSim2DEnvironment.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.simulation.physics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSim2DTransitionLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.Sim2DQAdapterLink;

import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.cgal.VoronoiDiagramCells;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DSectionPreprocessor;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class PhysicalSim2DEnvironment implements MobsimBeforeCleanupListener{

	private static final Logger log = Logger.getLogger(PhysicalSim2DEnvironment.class);

	@Deprecated
	private static final double DEP_BOX_WIDTH = 2./1.3; // must be >= agents' diameter

	private static final double ARRIVAL_AREA_LENGTH = 5; //must be a meaning full value (for pedestrians 5m seems to be fine)

	private final Sim2DEnvironment env;

	Map<Id,PhysicalSim2DSection> psecs = new HashMap<Id,PhysicalSim2DSection>();
	Map<Section,PhysicalSim2DSection> psecsSecs = new HashMap<Section,PhysicalSim2DSection>();

	@Deprecated
	Map<Id,PhysicalSim2DSection> linkIdPsecsMapping = new HashMap<Id,PhysicalSim2DSection>();
	private final Map<Id,Section> linkIdSecsMapping = new HashMap<Id,Section>();
	private final Sim2DScenario sim2dsc;


	private Map<Id<Link>, Sim2DQAdapterLink> lowResLinks;

	private final EventsManager eventsManager;

	//EXPERIMENTAL [GL Oct'13]
	private VoronoiDiagramCells<Sim2DAgent> vd;



	//	//EXPERIMENTAL multi threading stuff
	private final Poison poison = new Poison();
	private final int numOfThreads = 4; 
	private final CyclicBarrier kdSync = new CyclicBarrier(this.numOfThreads);
	private final CyclicBarrier cb = new CyclicBarrier(this.numOfThreads+1);
	private final List<PhysicalSim2DSectionUpdaterThread> threads = new ArrayList<PhysicalSim2DEnvironment.PhysicalSim2DSectionUpdaterThread>();

	private double time;

	private void awaitKDTreeSync() {
		try {
			this.kdSync.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	public PhysicalSim2DEnvironment(Sim2DEnvironment env, Sim2DScenario sim2dsc, EventsManager eventsManager) {
		this.env = env;
		this.sim2dsc = sim2dsc;
		this.eventsManager = eventsManager;
		if (Sim2DConfig.EXPERIMENTAL_VD_APPROACH) {
			this.vd = new VoronoiDiagramCells<Sim2DAgent>(env.getEnvelope(),eventsManager);
		}
		init();
	}


	private void init() {
		for (int i = 0; i < this.numOfThreads; i++) {
			PhysicalSim2DSectionUpdaterThread pt = new PhysicalSim2DSectionUpdaterThread(this.cb);
			this.threads.add(pt);
			new Thread(pt,this.env.getId().toString() + " PhysicalSim2DSectionUpdaterThread." + i).start();
		}

		for (Section sec : this.env.getSections().values()) {
			PhysicalSim2DSection psec = createAndAddPhysicalSection(sec);
			this.psecsSecs.put(sec, psec);
			for (Id id : sec.getRelatedLinkIds()) {
				this.linkIdPsecsMapping.put(id, psec);
				this.linkIdSecsMapping.put(id, sec);
			}
		}

	}

	private PhysicalSim2DSection createAndAddPhysicalSection(Section sec) {
		PhysicalSim2DSection psec = new PhysicalSim2DSection(sec,this.sim2dsc,this);
		this.psecs.put(sec.getId(),psec);
		return psec;
	}

	public PhysicalSim2DSection getPhysicalSim2DSectionAssociatedWithLinkId(Id id) {
		return this.linkIdPsecsMapping.get(id);
	}

	public Section getSectionAssociatedWithLinkId(Id id) {
		return this.linkIdSecsMapping.get(id);
	}

	public Collection<PhysicalSim2DSection> getPhysicalSim2DSections(){
		return this.psecs.values();
	}

	public double getTime() {
		return this.time;
	}

	public void doSimStep(double time) {
		this.time = time;
		//EXPERIMENTAL [GL Oct'13]
		if (Sim2DConfig.EXPERIMENTAL_VD_APPROACH) {
			//TODO this can be done more efficient, one just has to do some bookkeeping when agents enter/leave sim2d
			List<Sim2DAgent> allAgents = new ArrayList<Sim2DAgent>();
			for (PhysicalSim2DSection psec : this.psecs.values()) {
				allAgents.addAll(psec.getAgents());
			}
			this.vd.update(allAgents);
		}


		//		//single threaded
		//		for (PhysicalSim2DSection psec : this.psecs.values()) {
		//			psec.prepare();
		//		}
		//		for (PhysicalSim2DSection psec : this.psecs.values()) {
		//			psec.updateAgents(time);
		//		}

		//multi threaded
		this.cb.reset();
		for (PhysicalSim2DSectionUpdaterThread pt : this.threads) {
			pt.setTime(time);
		}
		int idx = 0;
		for (PhysicalSim2DSection psec : this.psecs.values()) {
			int tidx = idx % (this.numOfThreads);
			PhysicalSim2DSectionUpdaterThread pt = this.threads.get(tidx);
			pt.offer(psec);
			idx++;
		}
		for (PhysicalSim2DSectionUpdaterThread pt : this.threads) {
			pt.offer(this.poison);
		}
		try {
			this.cb.await();

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}


		for (PhysicalSim2DSection psec : this.psecs.values()) {
			psec.moveAgents(time);
		}

	}

	public void createAndAddPhysicalTransitionSection(
			QSim2DTransitionLink hiResLink) {

		Section sec = this.env.getSection(hiResLink.getLink());
		Id id = sec.getId();
		PhysicalSim2DSection psec = this.psecs.get(id);

		//retrieve opening
		LineSegment opening = null;
		Id oId = hiResLink.getLink().getFromNode().getId();
		opening = sec.getOpening(oId);
		Coord c = hiResLink.getLink().getFromNode().getCoord();
		double cx = c.getX();
		double cy = c.getY();

		Section s = sec.getNeighbor(opening);
		if (s != null) {
			log.info("transition area already exist: " + s.getId()); 
			PhysicalSim2DSection ta = getPhysicalSim2DSection(s);
			if (ta != null) {
				Point cc = s.getPolygon().getCentroid();
				double spawnX = cc.getX();
				double spawnY = cc.getY();

				hiResLink.createDepartureBox((TransitionAreaI) ta,spawnX,spawnY);
				return;
			}
		}


		//		for (LineSegment op : sec.getOpeningSegments()) {
		//			if (CGAL.isOnVector(cx, cy, op.x0, op.y0, op.x1, op.y1)){ 
		//				double cx1 = hiResLink.getLink().getToNode().getCoord().getX();
		//				double cy1 = hiResLink.getLink().getToNode().getCoord().getX();
		//				double left1 = CGAL.isLeftOfLine(op.x0, op.y0, cx, cy, cx1, cy1);
		//				double left2 = CGAL.isLeftOfLine(op.x1, op.y1, cx, cy, cx1, cy1);
		//				if (left1*left2 < 0) {
		//					opening = op;
		//					break;
		//				}
		//			}
		//		}

		if (opening == null) {
			double width = 8;
			double x0 = hiResLink.getLink().getToNode().getCoord().getX();
			double y0 = hiResLink.getLink().getToNode().getCoord().getY();
			double dx = x0 -cx;
			double dy = y0 -cy;
			double length = Math.sqrt(dx*dx+dy*dy);
			dx /= length;
			dy /= length;
			opening = new LineSegment();
			opening.x0 = (cx+x0)/2 + dy*width/2; //should be capacity dependent [GL July '13]
			opening.y0 = (cy+y0)/2 - dx*width/2;
			opening.x1 = (cx+x0)/2 - dy*width/2;
			opening.y1 = (cy+y0)/2 + dx*width/2;
			opening.dx = +dy;
			opening.dy = -dx;
		}

		double dx = opening.x1 - opening.x0;
		double dy = opening.y1 - opening.y0;
		double width = Math.sqrt(dx*dx+dy*dy);
		dx /= width;
		dy /= width;

		boolean ccw = CGAlgorithms.isCCW(sec.getPolygon().getExteriorRing().getCoordinates());
		double bx;
		double by;
		if (ccw) { // rotate right(currently not supported)
			throw new RuntimeException("Polygon describing section: " + sec.getId() + " has a counter clockwise orientation, which currently is not supported!");
		} else { // rotate left 
			bx = -dy;
			by = dx;
		}


		GeometryFactory geofac = new GeometryFactory();
		Coordinate c0 = new Coordinate(opening.x0,opening.y0);
		Coordinate c1 = new Coordinate(opening.x0+5*bx,opening.y0+5*by);
		Coordinate c2 = new Coordinate(opening.x1+5*bx,opening.y1+5*by);
		Coordinate c3 = new Coordinate(opening.x1,opening.y1);

		Coordinate [] coords = {c0,c1,c2,c3,c0};
		LinearRing lr = geofac.createLinearRing(coords);
		Polygon p = geofac.createPolygon(lr, null);

		Id hiResLinkId = hiResLink.getLink().getId();
		Id<Section> boxId = Id.create(id.toString() + "_link" + hiResLinkId + "_dep_box_", Section.class);
		int [] openings = {3,1};
		Id [] neighbors = {id};
		int level = sec.getLevel();
		s = this.env.createSection(boxId, p, openings, neighbors, level);
		Sim2DSectionPreprocessor.genLineSegments(s);

		double area = 5*width;
		//		double flowCap = hiResLink.getLink().getFromNode().getInLinks().values().iterator().next().getCapacity() / this.sim2dsc.getMATSimScenario().getNetwork().getCapacityPeriod();
		TransitionAreaII ta = new TransitionAreaII(s,this.sim2dsc,this,(int) (area*5.4));
		this.psecs.put(s.getId(),ta);
		this.psecsSecs.put(s, ta);


		LineSegment o = s.getOpeningSegments().get(1);
		s.addOpeningNeighborMapping(o,sec);
		sec.addOpeningNeighborMapping(opening, s);
		double spawnX = (c0.x+c2.x)/2;
		double spawnY = (c0.y+c2.y)/2;
		hiResLink.createDepartureBox(ta,spawnX,spawnY);

		////		//DEBUG
		//		for ( LineSegment bo : s.getObstacleSegments()) {
		//			this.eventsManager.processEvent(new LineEvent(0,bo,true,192,0,0,255,0));
		//		}
		//		} else {
		//			TransitionArea ta = new TransitionArea(s,this.sim2dsc,this,(int) flowCap+1);
		//			this.psecs.put(s.getId(),ta);
		//
		//
		//			Segment o = ta.getOpenings()[0];
		//			ta.putNeighbor(o,psec);
		//			hiResLink.createDepartureBox(ta,spawnX,spawnY);
		//
		//			//DEBUG
		//			for ( Segment bo : ta.getObstacles()) {
		//				this.eventsManager.processEvent(new LineEvent(0,bo,true,0,0,0,255,0));
		//			}			
		//		}

	}

	public Sim2DEnvironment getSim2DEnvironment() {
		return this.env;
	}

	public void registerLowResLinks(Map<Id<Link>, Sim2DQAdapterLink> lowResLinks2) {
		this.lowResLinks = lowResLinks2;

	}

	/*package*/ Sim2DQAdapterLink getLowResLink(Id nextLinkId) {
		return this.lowResLinks.get(nextLinkId);
	}

	public EventsManager getEventsManager() {
		return this.eventsManager;
	}

	private class PhysicalSim2DSectionUpdaterThread implements Runnable {

		private final CyclicBarrier barrier;
		private final BlockingQueue<PhysicalSim2DSection> queue = new LinkedBlockingQueue<PhysicalSim2DSection>();
		private double time;

		public PhysicalSim2DSectionUpdaterThread(CyclicBarrier barrier) {
			this.barrier = barrier;
		}

		@Override
		public void run() {
			PhysicalSim2DSection sec = null;
			Queue<PhysicalSim2DSection> secs = new LinkedList<PhysicalSim2DSection>();
			while (true) {
				try {
					sec = this.queue.take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (sec instanceof Poison) {
					PhysicalSim2DEnvironment.this.awaitKDTreeSync();
					//					if (true){
					//						throw new RuntimeException();
					//					}
					while (secs.peek() != null) {
						secs.poll().updateAgents(this.time);
					}
					try {
						this.barrier.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
				} else if (sec instanceof Kill){
					break;
				}else {
					sec.prepare();
					secs.add(sec);
				}
			}

		}

		private void setTime(double time) {
			this.time = time;
		}

		public void offer(PhysicalSim2DSection sec) {
			//			this.queue.a
			//			this.queue.offer(sec);
			try {
				this.queue.put(sec);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//			System.out.println(this.hashCode() + "    " + this.queue.size());
		}



	}

	private static class Poison extends PhysicalSim2DSection {

		//		public Poison(Section sec, Sim2DScenario sim2dsc,
		//				PhysicalSim2DEnvironment penv) {
		//			super(sec, sim2dsc, penv);
		//		}

	}
	private static class Kill extends PhysicalSim2DSection {

		//		public Poison(Section sec, Sim2DScenario sim2dsc,
		//				PhysicalSim2DEnvironment penv) {
		//			super(sec, sim2dsc, penv);
		//		}

	}


	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		for (PhysicalSim2DSectionUpdaterThread t : this.threads) {
			t.offer(new Kill());
		}
	}

	public PhysicalSim2DSection getPhysicalSim2DSection(Section n) {
		return this.psecsSecs.get(n);
	}

}
