package playground.gregor.sim2d_v2.simulation.floor;

import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CCWPolygon;

import com.vividsolutions.jts.geom.Coordinate;


//physical representation based an an ellipse
//see:
//@article{PhysRevE.82.046111,
//author = {Chraibi, Mohcine and Seyfried, Armin and Schadschneider, Andreas},
//volume = {82},
//journal = {Phys. Rev. E},
//month = {Oct},
//numpages = {9},
//title = {Generalized centrifugal-force model for pedestrian dynamics},
//year = {2010},
//url = {http://link.aps.org/doi/10.1103/PhysRevE.82.046111},
//doi = {10.1103/PhysRevE.82.046111},
//issue = {4},
//publisher = {American Physical Society},
//pages = {046111}
//}
public class VelocityDependentEllipse extends PhysicalAgentRepresentation {

	QuadTree<CCWPolygon> geometryQuad;
	
	CCWPolygon geometry;
	private final double a_min = .18;
	private final double b_min = .2;
	private final double b_max = .25;
	private final double tau_a = .53;
//	double a_min = .18;
//	double b_min = .2;
//	double b_max = .25;
//	double tau_a = .3;
	
	public VelocityDependentEllipse() {
		initGeometry();
	}
	
	private void initGeometry() {

		//x dim velo
		//y dim angle
		this.geometryQuad = new QuadTree<CCWPolygon>(0,0,2,360);

		for (double v = 0; v < this.maxV; v += this.maxV/24 ) {
			double a = this.a_min + this.tau_a * v;
			double b = this.b_max - (this.b_max - this.b_min)*v/1.34;
//			double a = this.a_min;
//			double b = this.b_max;
			Coordinate[] c = Algorithms.getEllipse(a, b);
			for (double angle = 0; angle < 360; angle += 15) {
				Coordinate [] tmp = new Coordinate[c.length];
				for (int i = 0; i < c.length-1; i++) {
					tmp[i] = new Coordinate(c[i]);
				}
				tmp[c.length-1] = tmp[0];
				double alpha = angle / 360. * 2 * Math.PI;
				Algorithms.rotate(alpha, tmp);
				CCWPolygon ccw = new CCWPolygon(tmp, new Coordinate(0,0), Math.max(a, b));
				this.geometryQuad.put(v, angle, ccw);
			}
		}
		this.geometry = this.geometryQuad.get(0, 0);
	}

	@Override
	public void update(double v, double alpha, Coordinate pos) {
		this.geometry = this.geometryQuad.get(v, alpha);
		this.geometry.translate(pos);
		
	}

	@Override
	public void translate(Coordinate pos) {
		this.geometry.translate(pos);
	}
	
	public CCWPolygon getGeometry(){
		return this.geometry;
	}
}
