package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d_v2.random.XORShiftRandom;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class RandomAlternativeVelocityChooser extends AlternativeVelocityChooser {

	private static final int NUM_OF_CANDIDATES = 20;
	private static final double MAX_V_COEFF = 1.25;
	private final XORShiftRandom random;

	private static final double W_I = 1.; //TODO shouldn't this be a parameter of the agents?

	public RandomAlternativeVelocityChooser() {
		this.random = new XORShiftRandom(MatsimRandom.getRandom().nextLong());
	}


	@Override
	public void chooseAlterantiveVelocity(List<VelocityObstacle> vOs, Coordinate c0, Coordinate c1, double[] df, Agent2D agent) {

//		//DEBUG
		GeometryFactory geofac = new GeometryFactory();
		
		double penalty = Double.POSITIVE_INFINITY;
		double bestVx = 0.1* df[0];
		double bestVy = 0.1* df[1];
		double candX = df[0];
		double candY = df[1];
		for (int i = 0; i < NUM_OF_CANDIDATES; i++){

			Coordinate candC = new Coordinate(c0.x + candX, c0.y + candY);
			
			//DEBUG
//			LineString ls = geofac.createLineString(new Coordinate[]{c0,candC});
			
			if (!Algorithms.testForCollision(vOs, candC)) {
				double testDist = candC.distance(c1);
//				GisDebugger.addGeometry(ls,"pen:"+(int)(100*testDist) + " time: NaN");
				
				if (testDist < penalty) {
					penalty = testDist;
					bestVx = candX;
					bestVy = candY;
				}
			} else {
				double colTime = timeToCollision(vOs, c0, candC);
				double testPen = candC.distance(c1) + W_I/colTime;
				
//				GisDebugger.addGeometry(ls,"pen:"+(int)(100*testPen) + " time:" + colTime);

				//FIXME use simStepTime instead of 0.04
				if (colTime > .04 && testPen < penalty) {
					penalty = testPen;
					bestVx = candX;
					bestVy = candY;
				} 
			}
			double r1 = this.random.nextDouble();
			candX = df[0]*.75 + r1 * agent.getDesiredVelocity();
			double r2 = this.random.nextDouble();
			candY = df[1]*.75 + r2 * agent.getDesiredVelocity();
		}
		
		
		df[0] = bestVx;
		df[1] = bestVy;

//		//DEBUG
//		LineString ls = geofac.createLineString(new Coordinate[]{c0,new Coordinate(c0.x+bestVx, c0.y+bestVy)});
//		GisDebugger.addGeometry(ls,"winner");
//		GisDebugger.dump("/Users/laemmel/devel/tmp/candidates.shp");
	}

}
