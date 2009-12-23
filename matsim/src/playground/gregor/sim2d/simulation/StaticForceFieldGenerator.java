package playground.gregor.sim2d.simulation;


import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d.controller.Sim2DConfig;
import playground.gregor.sim2d.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class StaticForceFieldGenerator {

	private static final double incr = 2*Math.PI/32;
	
	private final MultiPolygon structure;
	
	private final GeometryFactory geofac = new GeometryFactory();

	private StaticForceField ret;
	
	boolean loaded = false;

	private QuadTree<Force> forcesQuadTree;

	private Envelope envelope;
	
	public StaticForceFieldGenerator(MultiPolygon structure) {
		this.structure = structure;
	}
	
	public StaticForceField loadStaticForceField() {
		if (this.loaded) {
			return this.ret;
		}
		intQuadTree();
		calculateForces();
		return new StaticForceField(this.forcesQuadTree);
	}
	
	private void calculateForces() {
		for (double x = this.envelope.getMinX(); x <= this.envelope.getMaxX(); x += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
			for (double y = this.envelope.getMinY(); y <= this.envelope.getMaxY(); y += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
				Point point = this.geofac.createPoint(new Coordinate(x,y));
				if (!this.structure.covers(point) && this.structure.distance(point)>0.1) {
					Force f = calculateForce(x,y);
					if (f != null) {
						this.forcesQuadTree.put(f.getXCoord(), f.getYCoord(), f);
					}
				}
			}
		}
		
	}

	private Force calculateForce(double x, double y) {
		
		Force f = new Force(0,0,x,y);
		
		Coordinate c0 = new Coordinate(x,y);
		double alpha = 0;
		double cos = 0;
		double sin = 0;
		int contr = 0;
		for (; alpha < 2*Math.PI; ) {
			Coordinate [] coords = new Coordinate[4];
			coords[0] = c0;
			
			cos = Math.cos(alpha);
			sin = Math.sin(alpha);
			double x1 = x + cos * Sim2DConfig.Bw;
			double y1 = y + sin * Sim2DConfig.Bw;
			Coordinate c1 = new Coordinate(x1,y1);
			coords[1] = c1;
			
			alpha += incr;

			cos = Math.cos(alpha);
			sin = Math.sin(alpha);
			double x2 = x + cos * Sim2DConfig.Bw;
			double y2 = y + sin * Sim2DConfig.Bw;
			Coordinate c2 = new Coordinate(x2,y2);
			coords[2] = c2;
			coords[3] = c0;
			
			if(calcAndAddSectorForce(f,coords)) {
				contr++;
			}
		}
		if (contr == 0) {
			return null;
		}
		f.setFx(f.getFx()/contr);
		f.setFy(f.getFy()/contr);
		
		
		Coordinate [] cooo = new Coordinate[] {new Coordinate(f.getXCoord(),f.getYCoord()),new Coordinate(f.getXCoord()+0.01,f.getYCoord()+0.01),new Coordinate(f.getXCoord()+f.getFx(),f.getYCoord()+f.getFy()),new Coordinate(f.getXCoord(),f.getYCoord())};
		LinearRing lr = this.geofac.createLinearRing(cooo);
		Polygon ppp = this.geofac.createPolygon(lr, null);
//		GisDebugger.addGeometry(ppp);
//		GisDebugger.dump("../../tmp/staticForcesDbg.shp");
		return f;
	}

	private boolean calcAndAddSectorForce(Force f, Coordinate[] coords) {
		Polygon p = this.geofac.createPolygon(this.geofac.createLinearRing(coords), null);
		Geometry g = this.structure.intersection(p);
		if (!(g instanceof GeometryCollection)) {
			DistanceOp op = new DistanceOp(g,this.geofac.createPoint(coords[0]));
			Coordinate[] tmp = op.closestPoints();
			double fX = tmp[1].x - tmp[0].x;
			double fY = tmp[1].y - tmp[0].y;
			double dist = Math.sqrt(Math.pow(fX,2)+Math.pow(fY,2 ));
			if (dist > Sim2DConfig.Bw) {
				throw new RuntimeException("this should not happen!!");
			} else if (dist <= 0.01) {
				return false;
			}
			
			//DEBUG
//			GisDebugger.addGeometry(p);
//			Coordinate[] tmp1 = new Coordinate[]{tmp[0],new Coordinate(tmp[0].x+0.01,tmp[0].y+0.01),tmp[1],tmp[0]};
//			LinearRing lr = this.geofac.createLinearRing(tmp1);
//			Polygon tmpP = this.geofac.createPolygon(lr, null);
//			GisDebugger.addGeometry(tmpP);
			
			double exp = Math.exp(Sim2DConfig.Bw/dist);
			fX *= exp/dist;
			fY *= exp/dist;
//			fX /= dist;
//			fY /= dist;
			
//			double l2 = Math.sqrt(Math.pow(fX, 2)+Math.pow(fY, 2));
//			System.out.println(dist + " " + l2);
			
			f.setFx(f.getFx()+fX);
			f.setFy(f.getFy()+fY);
			return true;
		}
		
		return false;
		
	}

	private void intQuadTree() {
		Geometry geo = this.structure.getEnvelope();
		this.envelope = new Envelope();
		for (Coordinate c : geo.getCoordinates()) {
			this.envelope.expandToInclude(c);
		}
		this.forcesQuadTree = new QuadTree<Force>(this.envelope.getMinX(),this.envelope.getMinY(),this.envelope.getMaxX(),this.envelope.getMaxY());
	}

//	@Deprecated
//	public StaticForceField oldloadStaticForceField() {
//	
//		if (this.loaded) {
//			return this.ret;
//		}
//		
//		Geometry geo = this.structure.getEnvelope();
//		Envelope e = new Envelope();
//		for (Coordinate c : geo.getCoordinates()) {
//			e.expandToInclude(c);
//		}
//		GeometryFactory geofac = new GeometryFactory();
//		QuadTree<ForcePoint> tmpTree = new QuadTree<ForcePoint>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
//		QuadTree<Force> q = new QuadTree<Force>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
//		this.ret = new StaticForceField(q);
//		int numOfGeos = 0;
//		for (double x = e.getMinX(); x <= e.getMaxX(); x += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
//			for (double y = e.getMinY(); y <= e.getMaxY(); y += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
//				//				this.structure.distance(g)
//				DistanceOp op = new DistanceOp(this.structure,geofac.createPoint(new Coordinate(x,y)));
//				Coordinate[] coords = op.closestPoints();
//				ForcePoint force = new ForcePoint();
//				force.fX = coords[0].x - coords[1].x;
//				force.fY = coords[0].y - coords[1].y;
//				if (force.fX == 0 && force.fY == 0) {
//					continue;
//				}
//				double length = Math.sqrt(Math.pow(force.fX,2)+Math.pow(force.fY,2 ));
//				if (length >Sim2DConfig.Bw) {
//					continue;
//				} else {
//					double exp = Math.exp(length/Sim2DConfig.Bw);
//					force.fX *= -exp/length;
//					force.fY *= -exp/length;
//				}
//				force.x  = x;
//				force.y  = y;
//				force.tmpX = force.fX;
//				force.tmpY = force.fY;
//				tmpTree.put(x, y, force);
//				numOfGeos++;
//			}
//			this.loaded = true;	
//		}
//		for (ForcePoint fp : tmpTree.values()) {
//			Collection<ForcePoint> coll = tmpTree.get(fp.x,fp.y, Sim2DConfig.Bw);
//			double denom = 1;
//			for (ForcePoint tmp : coll) {
//				if (isVisible(tmp,fp)) {
//					double weight = 1- tmp.distance(fp)/Sim2DConfig.Bw;
//					fp.fX += weight * tmp.tmpX;
//					fp.fY += weight * tmp.tmpY;
//					denom += weight;
//				}
//			}
//			fp.fX /= denom;
//			fp.fY /= denom;
//			this.ret.addForce(new Force(fp.fX,fp.fY,fp.x,fp.y));
//		}
//		return this.ret;
//	}

//	private boolean isVisible(ForcePoint fp1, ForcePoint fp2) {
//		LineString ls = this.geofac.createLineString(new Coordinate[]{new Coordinate(fp1.x,fp1.y),new Coordinate(fp2.x,fp2.y)});
//		if (ls.intersects(this.structure)) {
//			return false;
//		}
//		return true;
//	}


	
	
//	private static class ForcePoint {
//		double x;
//		double y;
//		
//		//TODO remove this!!
//		double tmpX;
//		double tmpY;
//		
//		
//		double fX;
//		double fY;
//		public double distance(ForcePoint fp) {
//			return Math.sqrt(Math.pow(fp.x-this.x, 2)+Math.pow(fp.y-this.y, 2));
//		}
//	}
	
}
