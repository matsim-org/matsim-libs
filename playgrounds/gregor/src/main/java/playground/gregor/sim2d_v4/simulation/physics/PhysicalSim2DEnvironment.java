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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSim2DTransitionLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.Sim2DQTransitionLink;

import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.LinkInfo;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class PhysicalSim2DEnvironment {

	private static final Logger log = Logger.getLogger(PhysicalSim2DEnvironment.class);

	private static final float DEP_BOX_WIDTH = .5f; // must be >= agents' diameter

	private final Sim2DEnvironment env;

	Map<Id,PhysicalSim2DSection> psecs = new HashMap<Id,PhysicalSim2DSection>();
	Map<Id,PhysicalSim2DSection> linkIdPsecsMapping = new HashMap<Id,PhysicalSim2DSection>();

	private final Sim2DScenario sim2dsc;

	private final double offsetX;

	private final double offsetY;

	//DEBUG
	private static final VisDebugger visDebugger = new VisDebugger();

	private Map<Id, Sim2DQTransitionLink> lowResLinks;

	private final EventsManager eventsManager;

	public PhysicalSim2DEnvironment(Sim2DEnvironment env, Sim2DScenario sim2dsc, EventsManager eventsManager) {
		this.env = env;
		this.offsetX = env.getEnvelope().getMinX();
		this.offsetY = env.getEnvelope().getMinY();
		this.sim2dsc = sim2dsc;
		this.eventsManager = eventsManager;
		init();
	}

	private void init() {
		for (Section sec : this.env.getSections().values()) {
			PhysicalSim2DSection psec = createAndAddPhysicalSection(sec);
			for (Id id : sec.getRelatedLinkIds()) {
				this.linkIdPsecsMapping.put(id, psec);
			}
		}

	}

	private PhysicalSim2DSection createAndAddPhysicalSection(Section sec) {
		PhysicalSim2DSection psec = new PhysicalSim2DSection(sec,this.sim2dsc,this.offsetX,this.offsetY,this);
		this.psecs.put(sec.getId(),psec);
		return psec;
	}
	
	private DepartureBox createAndAddDepartureBox(Section sec) {
		DepartureBox psec = new DepartureBox(sec,this.sim2dsc,this.offsetX,this.offsetY,this);
		this.psecs.put(sec.getId(),psec);
		return psec;
	}

	/*package*/ PhysicalSim2DSection getPhysicalSim2DSectionAssociatedWithLinkId(Id id) {
		return this.linkIdPsecsMapping.get(id);
	}

	public void doSimStep(double time) {
		//		log.warn("not implemented yet!");
		for (PhysicalSim2DSection psec : this.psecs.values()) {
			psec.updateAgents();
		}
		for (PhysicalSim2DSection psec : this.psecs.values()) {
			psec.moveAgents(time);
		}

		//DEBUG
		boolean hasAgent = false;
		for (PhysicalSim2DSection psec : this.psecs.values()) {
			if (psec.getNumberOfAllAgents() > 0) {
				hasAgent = true;
				break;
			}
		}
		if (hasAgent){
			for (PhysicalSim2DSection psec : this.psecs.values()) {
				psec.debug(visDebugger);
			}
			long timel = System.currentTimeMillis();
			long last = visDebugger.lastUpdate;
			long diff = timel - last;
			if (diff < 100) {
				long wait = 100-diff;
				try {
					Thread.sleep(wait);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			

			visDebugger.setTime(time);
			visDebugger.update();
			visDebugger.lastUpdate = System.currentTimeMillis();
		}
		//		}
		//		log.info("sim step done.");

		//1. update agents' world model (i.e. neighbors, obstacles)
		//2. update agents' new velocity/acceleration (here the different simulation models will be applied)
		//3. move agents' 
		//3. b if agent enters new section:
		//			precompute relevant exit of new section (needed to recognize that an agent leaves a section) (may be a list of sections to traverse calculated at the beginning would be helpful)
		//			unregister agent in current section (it.remove());
		//			register agent in next section (.add(agent) only if sections are not multi-threaded)
	}

	public void createAndAddPhysicalTransitionSections(
			QSim2DTransitionLink hiResLink) {
		Section sec = this.env.getSection(hiResLink.getLink());
		Id id = sec.getId();
		PhysicalSim2DSection psec = this.psecs.get(id);

		//retrieve opening
		LinkInfo li = psec.getLinkInfo(hiResLink.getLink().getId());
		Segment opening = li.fromOpening;

		//create dbox link info
		LinkInfo dboxLi = new LinkInfo();
		dboxLi.dx = li.dx;
		dboxLi.dy = li.dy;
		Segment dboxFl = new Segment();
		dboxFl.x1 = li.fromOpening.x0; 
		dboxFl.x0 = li.fromOpening.x1;
		dboxFl.y1 = li.fromOpening.y0;
		dboxFl.y0 = li.fromOpening.y1;
		dboxLi.finishLine = dboxFl;

		//create departure boxes
		float dx = opening.x1 - opening.x0;
		float dy = opening.y1 - opening.y0;
		float width = (float) Math.sqrt(dx*dx+dy*dy);
		dx /= width;
		dy /= width;

		boolean ccw = CGAlgorithms.isCCW(sec.getPolygon().getExteriorRing().getCoordinates());
		float bx;
		float by;
		if (ccw) { // rotate right(currently not supported)
			bx = dy;
			by = -dx;
			throw new RuntimeException("Polygon describing section: " + sec.getId() + " has a counter clockwise orientation, which currently is not supported!");
		} else { // rotate left 
			bx = -dy;
			by = dx;
		}

		bx *= 1.5f * DEP_BOX_WIDTH;
		by *= 1.5f * DEP_BOX_WIDTH;

		dx *= DEP_BOX_WIDTH;
		dy *= DEP_BOX_WIDTH;

		int nrOfBoxes = (int) (width / DEP_BOX_WIDTH + .5f);
		double bottomX = opening.x0+this.offsetX;
		double bottomY = opening.y0+this.offsetY;
		GeometryFactory geofac = new GeometryFactory();
		for (int i = 0; i < nrOfBoxes; i++) {
			Coordinate c0 = new Coordinate(bottomX,bottomY);
			Coordinate c1 = new Coordinate(bottomX+bx,bottomY+by);
			Coordinate c2 = new Coordinate(bottomX+bx+dx,bottomY+by+dy);
			Coordinate c3 = new Coordinate(bottomX+dx,bottomY+dy);
			Coordinate [] coords = {c0,c1,c2,c3,c0};
			LinearRing lr = geofac.createLinearRing(coords);
			Polygon p = geofac.createPolygon(lr, null);

			Id hiResLinkId = hiResLink.getLink().getId();
			Id boxId = this.sim2dsc.getMATSimScenario().createId(id.toString() + "_link" + hiResLinkId + "_dep_box_" + i);
			int [] openings = {3};
			Id [] neighbors = {id};
			int level = sec.getLevel();
			Section s = this.env.createSection(boxId, p, openings, neighbors, level);
			float spawnX = (float)(bottomX-this.offsetX) + bx/1.5f + dx/2;
			float spawnY = (float)(bottomY-this.offsetY) + by/1.5f + dy/2;
			PhysicalSim2DSection psecBox = createAndAddDepartureBox(s);
			psecBox.putLinkInfo(hiResLinkId, dboxLi);
			Segment o = psecBox.getOpenings()[0];
			psecBox.putNeighbor(o,psec);
			hiResLink.createDepartureBox(psecBox,spawnX,spawnY);
			bottomX += dx;
			bottomY += dy;
		}

	}

	public void registerLowResLinks(Map<Id, Sim2DQTransitionLink> lowResLinks2) {
		this.lowResLinks = lowResLinks2;
		
	}

	/*package*/ Sim2DQTransitionLink getLowResLink(Id nextLinkId) {
		return this.lowResLinks.get(nextLinkId);
	}
	
	/*package*/ EventsManager getEventsManager() {
		return this.eventsManager;
	}


}
