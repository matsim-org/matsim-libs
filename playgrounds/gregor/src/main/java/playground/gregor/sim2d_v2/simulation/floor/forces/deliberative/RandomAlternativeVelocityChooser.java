package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d_v2.random.XORShiftRandom;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class RandomAlternativeVelocityChooser extends AlternativeVelocityChooser {

	private static final int NUM_OF_CANDIDATES = 9;
	private static final double MAX_V_COEFF = 1.25;
	private final XORShiftRandom random;

	private static final double W_I = 5.; //TODO shouldn't this be a parameter of the agents?
	
	private final List<C> cs = new ArrayList<C>();

	public RandomAlternativeVelocityChooser() {
		this.random = new XORShiftRandom(MatsimRandom.getRandom().nextLong());
		init();
	}


	private void init() {
		
		
		for (double alpha = 0; alpha <= 2*Math.PI; alpha += Math.PI/4) {
			double x = Math.cos(alpha);
			double y = Math.sin(alpha);
			C c = new C();
			c.x = x;
			c.y = y;
			this.cs.add(c);
			
		}
		
	}


	@Override
	public void chooseAlterantiveVelocity(List<? extends VelocityObstacle> vOs, Coordinate c0, Coordinate c1, double[] df, Agent2D agent) {

//		//DEBUG
		GeometryFactory geofac = new GeometryFactory();
		
		
		
		double penalty = Double.POSITIVE_INFINITY;
		double bestVx = 0;
		double bestVy = 0;
		double[] oldBest = agent.getOldBest();
		double candX = oldBest[0];
		double candY = oldBest[1];
		for (int i = 0; i < NUM_OF_CANDIDATES; i++){

			double vxNext = agent.getVx() + 0.04*(candX - agent.getVx())/0.5;
			double vyNext = agent.getVy() + 0.04*(candY - agent.getVy())/0.5;
		
//			double stopTime = Math.hypot(vxNext, vyNext)/0.5;
			double stopTime = Math.sqrt(vxNext*vxNext + vyNext*vyNext)/0.5; //TODO 0.5 has to be replaced by tau!!!
			Coordinate candC = new Coordinate(c0.x + vxNext, c0.y + vyNext);
			
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
				if (colTime > stopTime && testPen < penalty) {
					penalty = testPen;
					bestVx = candX;
					bestVy = candY;
				} 
			}
//			double r = MatsimRandom.getRandom().nextDouble();
//			C c = this.cs.get(i);
//			candX = df[0]*.75 + r * agent.getDesiredVelocity() * c.x;
//			candY = df[1]*.75 + r * agent.getDesiredVelocity() * c.y;
			double r1 = this.random.nextDouble();
			candX = df[0]*.75 + r1 * agent.getDesiredVelocity();
			double r2 = this.random.nextDouble();
			candY = df[1]*.75 + r2 * agent.getDesiredVelocity();
		}
		
		
		df[0] = bestVx;
		df[1] = bestVy;
		agent.setOldBest(df);

//		//DEBUG
//		LineString ls = geofac.createLineString(new Coordinate[]{c0,new Coordinate(c0.x+bestVx, c0.y+bestVy)});
//		GisDebugger.addGeometry(ls,"winner");
//		GisDebugger.dump("/Users/laemmel/devel/tmp/candidates.shp");
	}

	private static class C{
		double x;
		double y;
	}
}
