package playground.gregor.scenariogen.hhw3hybrid;

import org.apache.log4j.Logger;

import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Crossing;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Goal;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Polygon;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Room;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Subroom;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Transition;
import playground.gregor.scenariogen.hhw3hybrid.JuPedSimGeomtry.Vertex;

public class JupedSimGeometryOffsetter {
	
	private static final Logger log = Logger.getLogger(JupedSimGeometryOffsetter.class);
	

	private JuPedSimGeomtry geo;

	public JupedSimGeometryOffsetter(JuPedSimGeomtry geo){
		
		this.geo = geo;
		
	}
	
	public void run(){
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		
		for (Room r : this.geo.rooms) {
			for (Subroom s : r.subrooms) {
				for (Polygon p : s.polygons){
					for (Vertex v : p.vertices){
						if (v.px < minX) {
							minX = v.px;
						}
						if (v.py < minY) {
							minY = v.py;
						}
					}
				}
			}
			for (Crossing c : r.crossings) {
				if (c.v1.px < minX) {
					minX = c.v1.px;
				}
				if (c.v1.py < minY) {
					minY = c.v1.py;
				}
				if (c.v2.px < minX) {
					minX = c.v2.px;
				}
				if (c.v2.py < minY) {
					minY = c.v2.py;
				}
			}
		}
		for (Transition c : this.geo.transitions) {
			if (c.v1.px < minX) {
				minX = c.v1.px;
			}
			if (c.v1.py < minY) {
				minY = c.v1.py;
			}
			if (c.v2.px < minX) {
				minX = c.v2.px;
			}
			if (c.v2.py < minY) {
				minY = c.v2.py;
			}
		}
		
		for (Goal g : this.geo.goals) {
			for (Vertex v : g.p.vertices) {
				if (v.px < minX) {
					minX = v.px;
				}
				if (v.py < minY) {
					minY = v.py;
				}
			}
		}
			
		log.info(minX + " " + minY);
		
		for (Room r : this.geo.rooms) {
			for (Subroom s : r.subrooms) {
				for (Polygon p : s.polygons){
					for (Vertex v : p.vertices){
						v.px -= minX;
						v.py -= minY;
					}
				}
			}
			for (Crossing c : r.crossings) {
				c.v1.px -= minX;
				c.v1.py -= minY;
				c.v2.px -= minX;
				c.v2.py -= minY;
			}
		}
		for (Transition c : this.geo.transitions) {
			c.v1.px -= minX;
			c.v1.py -= minY;
			c.v2.px -= minX;
			c.v2.py -= minY;
		}
		
		for (Goal g : this.geo.goals) {
			for (Vertex v : g.p.vertices) {
				v.px -= minX;
				v.py -= minY;
			}
		}
		
	}
}
