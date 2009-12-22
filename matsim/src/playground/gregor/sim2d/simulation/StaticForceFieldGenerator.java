package playground.gregor.sim2d.simulation;

import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d.controller.Sim2DConfig;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class StaticForceFieldGenerator {


	private final MultiPolygon structure;
	
	private final GeometryFactory geofac = new GeometryFactory();

	private QuadTree<Force> ret;
	
	boolean loaded = false;
	
	public StaticForceFieldGenerator(MultiPolygon structure) {
		this.structure = structure;
	}
	
	public QuadTree<Force> loadStaticForceField() {
	
		if (this.loaded) {
			return this.ret;
		}
		
		Geometry geo = this.structure.getEnvelope();
		Envelope e = new Envelope();
		for (Coordinate c : geo.getCoordinates()) {
			e.expandToInclude(c);
		}
		GeometryFactory geofac = new GeometryFactory();
		QuadTree<ForcePoint> tmpTree = new QuadTree<ForcePoint>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		this.ret = new QuadTree<Force>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		int numOfGeos = 0;
		for (double x = e.getMinX(); x <= e.getMaxX(); x += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
			for (double y = e.getMinY(); y <= e.getMaxY(); y += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
				//				this.structure.distance(g)
				DistanceOp op = new DistanceOp(this.structure,geofac.createPoint(new Coordinate(x,y)));
				Coordinate[] coords = op.closestPoints();
				ForcePoint force = new ForcePoint();
				force.fX = coords[0].x - coords[1].x;
				force.fY = coords[0].y - coords[1].y;
				if (force.fX == 0 && force.fY == 0) {
					continue;
				}
				double length = Math.sqrt(Math.pow(force.fX,2)+Math.pow(force.fY,2 ));
				if (length >Sim2DConfig.Bw) {
					continue;
				} else {
					double exp = Math.exp(length/Sim2DConfig.Bw);
					force.fX *= -exp/length;
					force.fY *= -exp/length;
				}
				force.x  = x;
				force.y  = y;
				force.tmpX = force.fX;
				force.tmpY = force.fY;
				tmpTree.put(x, y, force);
				numOfGeos++;
			}
			this.loaded = true;	
		}
		for (ForcePoint fp : tmpTree.values()) {
			Collection<ForcePoint> coll = tmpTree.get(fp.x,fp.y, Sim2DConfig.Bw);
			double denom = 1;
			for (ForcePoint tmp : coll) {
				if (isVisible(tmp,fp)) {
					double weight = 1- tmp.distance(fp)/Sim2DConfig.Bw;
					fp.fX += weight * tmp.tmpX;
					fp.fY += weight * tmp.tmpY;
					denom += weight;
				}
			}
			fp.fX /= denom;
			fp.fY /= denom;
			this.ret.put(fp.x, fp.y, new Force(fp.fX,fp.fY,fp.x,fp.y));
		}
		return this.ret;
	}

	private boolean isVisible(ForcePoint fp1, ForcePoint fp2) {
		LineString ls = this.geofac.createLineString(new Coordinate[]{new Coordinate(fp1.x,fp1.y),new Coordinate(fp2.x,fp2.y)});
		if (ls.intersects(this.structure)) {
			return false;
		}
		return true;
	}


	
	
	private static class ForcePoint {
		double x;
		double y;
		
		//TODO remove this!!
		double tmpX;
		double tmpY;
		
		
		double fX;
		double fY;
		public double distance(ForcePoint fp) {
			return Math.sqrt(Math.pow(fp.x-this.x, 2)+Math.pow(fp.y-this.y, 2));
		}
	}
	
}
