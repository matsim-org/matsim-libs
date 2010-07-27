package playground.gregor.sim2d.peekabot;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d.controller.Sim2DConfig;
import playground.gregor.sim2d.simulation.Agent2D;
import playground.gregor.sim2d.simulation.Floor;
import playground.gregor.sim2d.simulation.Force;
import playground.gregor.sim2d.simulation.Network2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Sim2DVis {



	private static final double TWO_PI = 2 * Math.PI;
	private static final double PI_HALF =  Math.PI / 2;
	private static final float FLOOR_HEIGHT = 3;

	private long oldSystemTime = 0;

	private double ofX;
	private double ofY;
	private PeekABotClient peekabot;
	private Network2D network2d;

	private int segmentCount = 0;

	private Set<Id> activeAgents = new java.util.HashSet<Id>();

	public Sim2DVis(double ofX, double ofY) {
		this.ofX = ofX;
		this.ofY = ofY;
		this.peekabot = new PeekABotClient();
	}


	public void reset() {

		for (Id id : this.activeAgents ) {
			this.peekabot.removeBot(Integer.parseInt(id.toString()));
		}
		this.activeAgents.clear();
	}



	public void draw(double time) {

		long wait = (long) ((this.oldSystemTime + (1000*Sim2DConfig.TIME_STEP_SIZE)) - System.currentTimeMillis());
		if (wait > 0) {
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		this.oldSystemTime = System.currentTimeMillis();			
		for (Floor floor : this.network2d.getFloors()) {
			draw(floor);
		}


	}


	private void draw(Floor floor) {
		for (Agent2D agent : floor.getAgents()) {
			if (this.activeAgents.contains(agent.getId())) {
				setAgentPos(floor,agent);
			} else {
				this.activeAgents.add(agent.getId());
				initAgent(agent);
			}

		}
	}


	private void initAgent(Agent2D agent) {
		Coordinate coord = agent.getPosition();
		this.peekabot.addBot(Integer.parseInt(agent.getId().toString()), (float)(coord.x-this.ofX), (float)(coord.y-this.ofY), (float)(coord.z));
		this.peekabot.setBotColor(Integer.parseInt(agent.getId().toString()), MatsimRandom.getRandom().nextFloat(), MatsimRandom.getRandom().nextFloat(), MatsimRandom.getRandom().nextFloat());

	}


	private void setAgentPos(Floor floor, Agent2D agent) {
		Coordinate coord = agent.getPosition();
		//		double velocity = floor.getAgentVelocity(agent);
		Force f = floor.getAgentForce(agent);
		double alpha = getPhaseAngle(f);
		//		alpha /= TWO_PI;
		//		alpha *= 360;
		//		alpha += 90;

		this.peekabot.setBotPosition(Integer.parseInt(agent.getId().toString()), (float)(coord.x-this.ofX), (float)(coord.y-this.ofY), 0.1f, (float)(alpha));

	}


	public void setNetwork2D(Network2D network2d) {
		this.network2d = network2d;
	}


	public void drawFloorPlans(Map<MultiPolygon, List<Link>> mps) {



		for (MultiPolygon mp : mps.keySet()) {
			drawFloor(mp);
			this.peekabot.init();	
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				Geometry geo = mp.getGeometryN(i);
				if (geo instanceof Polygon) {
					drawPolygon((Polygon)geo);
				}

			}


		}

		this.peekabot.init();

		//    	this.peekABotVis.initPolygon(++count, 4, 1, .5f, 0, 0);
		//    	this.peekABotVis.addPolygonCoord(count,minX, minY, 0);
		//    	this.peekABotVis.addPolygonCoord(count,minX, maxY, 0);
		//    	this.peekABotVis.addPolygonCoord(count,maxX, maxY, 0);
		//    	this.peekABotVis.addPolygonCoord(count,maxX, minY, 0);
		//		
		//		this.peekABotVis.init();

	}



	private void drawFloor(MultiPolygon mp) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		Geometry b = mp.getBoundary();
		Coordinate[] c = b.getCoordinates();
		for (int i = 0; i < c.length; i++) {
			if (c[i].x < minX) {
				minX = c[i].x;
			} else 	if (c[i].y < minY) {
				minY = c[i].y;
			} else 	if (c[i].y > maxY) {
				maxY = c[i].y;
			} else 	if (c[i].x > maxX) {
				maxX = c[i].x;
			}
		}
		this.peekabot.initPolygon(this.segmentCount, 4,  1, .5f, 0, 0);
		float x1 = (float) (minX-this.ofX);
		float x2 = (float) (maxX-this.ofX);
		float y1 = (float) (minY-this.ofY);
		float y2 = (float) (maxY-this.ofY);
		this.peekabot.addPolygonCoord(this.segmentCount, x1, y1, 0);
		this.peekabot.addPolygonCoord(this.segmentCount, x2, y1, 0);
		this.peekabot.addPolygonCoord(this.segmentCount, x2, y2, 0);
		this.peekabot.addPolygonCoord(this.segmentCount++, x1, y2, 0);
	}


	private void drawPolygon(Polygon p) {
		LineString lr = p.getExteriorRing();

		for (int i = 0; i < lr.getNumPoints()-1; i++) {
			drawSegment(lr.getPointN(i), lr.getPointN(i+1));
		}
		for (int i = 0; i < p.getNumInteriorRing(); i++) {
			lr = p.getInteriorRingN(i);
			for (int j = 0; j < lr.getNumPoints()-1; j++) {
				drawSegment(lr.getPointN(j), lr.getPointN(j+1));
			}
		}

		//		drawSegment(lr.getPointN(lr.getNumPoints()-1), lr.getPointN(0));
	}


	private void drawSegment(Point pointN1, Point pointN2) {
		this.peekabot.initPolygon(this.segmentCount, 5, .75f, .75f , .75f, 3);
		float x1 = (float) (pointN1.getX()-this.ofX);
		float y1 = (float) (pointN1.getY()-this.ofY);
		float x2 = (float) (pointN2.getX()-this.ofX);
		float y2 = (float) (pointN2.getY()-this.ofY);
		this.peekabot.addPolygonCoord(this.segmentCount,x1 , y1, 0);
		this.peekabot.addPolygonCoord(this.segmentCount,x2 , y2, 0);
		this.peekabot.addPolygonCoord(this.segmentCount,x2 , y2, FLOOR_HEIGHT);
		this.peekabot.addPolygonCoord(this.segmentCount,x1 , y1, FLOOR_HEIGHT);
		this.peekabot.addPolygonCoord(this.segmentCount++,x1 , y1, 0);

	}


	private double getPhaseAngle(Force f) {
		double alpha = 0.0;
		if (f.getFx() > 0) {
			alpha = Math.atan(f.getFy()/f.getFx());
		} else if (f.getFx() < 0) {
			alpha = Math.PI + Math.atan(f.getFy()/f.getFx());
		} else { // i.e. DX==0
			if (f.getFy() > 0) {
				alpha = PI_HALF;
			} else {
				alpha = -PI_HALF;
			}
		}
		if (alpha < 0.0) alpha += TWO_PI;
		return alpha;
	}

	//	private void drawFloorPlan() {
	////    	this.peekABotVis.initPolygon(1, 4, 1, .5f, 0, 0);
	////    	this.peekABotVis.addPolygonCoord(1,10, 10, 3);
	////    	this.peekABotVis.addPolygonCoord(1,10, 0, 3);
	////    	this.peekABotVis.addPolygonCoord(1,0, 0, 3);
	////    	this.peekABotVis.addPolygonCoord(1,0, 15, 3);
	////    	this.peekABotVis.init();
	//		int count = 0;
	//		float minX = Float.POSITIVE_INFINITY;
	//		float minY = Float.POSITIVE_INFINITY;
	//		float maxX = Float.NEGATIVE_INFINITY;
	//		float maxY = Float.NEGATIVE_INFINITY;
	//		
	////		int wallCount = 0;
	//		for (Entry<MultiPolygon, List<Link>> e : this.floors.entrySet()) {
	//			MultiPolygon mp = e.getKey();
	//			for (int i = 0; i < mp.getNumGeometries(); i++) {
	//				Geometry geo = mp.getGeometryN(i);
	//				Coordinate[] coords = geo.getCoordinates();
	//				this.peekABotVis.initPolygon(++count, coords.length, 0.5f, 0.5f, 0.5f, 3);
	//				for (int j = 0; j < coords.length; j++) {
	//					float x = (float) (coords[j].x-this.offX);
	//					if (x < minX) {
	//						minX = x;
	//					} else if (x > maxX) {
	//						maxX = x;
	//					}
	//					
	//					float y = (float) (coords[j].y-this.offY);
	//					if (y < minY) {
	//						minY = y;
	//					} else if (y > maxY) {
	//						maxY = y;
	//					}
	//					float z = (float) coords[j].z;
	//					this.peekABotVis.addPolygonCoord(count, x, y, 0);
	//					if (j > 1) {
	//						float x0 = (float) (coords[j-1].x-this.offX);
	//						float x1 = (float) (coords[j].x-this.offX);
	//						float y0 = (float) (coords[j-1].y-this.offY);
	//						float y1 = (float) (coords[j].y-this.offY);
	//						this.peekABotVis.initPolygon(++count, coords.length, 0.5f, 0.5f, 0.5f, 3);
	//						this.peekABotVis.addPolygonCoord(count, x0, y0, 0);
	//						this.peekABotVis.addPolygonCoord(count, x1, y1, 0);
	//						this.peekABotVis.addPolygonCoord(count, x1, y1, 3);
	//						this.peekABotVis.addPolygonCoord(count, x0, y0, 3);
	//						this.peekABotVis.addPolygonCoord(count, x0, y0, 0);
	//					}
	//				}
	//			}
	//		}
	////		for (Entry<MultiPolygon, List<Link>> e : this.floors.entrySet()) {
	////			MultiPolygon mp = e.getKey();
	////			for (int i = 0; i < mp.getNumGeometries(); i++) {
	////				Geometry geo = mp.getGeometryN(i);
	////				Coordinate[] coords = geo.getCoordinates();
	////				this.peekABotVis.initPolygon(++count, coords.length, 0.5f, 0.5f, 0.5f, 3);
	////				for (int j = 0; j < coords.length; j++) {
	////					float x = (float) (coords[j].x-this.offX);
	////					float y = (float) (coords[j].y-this.offY);
	////					float z = (float) coords[j].z;
	////					this.peekABotVis.addPolygonCoord(count, x, y, 3);
	////				}
	////			}
	////		}
	//		
	//    	this.peekABotVis.initPolygon(++count, 4, 1, .5f, 0, 0);
	//    	this.peekABotVis.addPolygonCoord(count,minX, minY, 0);
	//    	this.peekABotVis.addPolygonCoord(count,minX, maxY, 0);
	//    	this.peekABotVis.addPolygonCoord(count,maxX, maxY, 0);
	//    	this.peekABotVis.addPolygonCoord(count,maxX, minY, 0);
	//		
	//		this.peekABotVis.init();
	//	}

}

