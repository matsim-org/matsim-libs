package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.random.XORShiftRandom;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;

public class RandomAlternativeVelocityChooser extends AlternativeVelocityChooser {

	private static final int NUM_OF_CANDIDATES = 100;
	private static final double MAX_V_COEFF = 1.25;
	private final XORShiftRandom random;

	private static final double W_I = 5.; //TODO shouldn't this be a parameter of the agents?
	
	private final List<C> cs = new ArrayList<C>();
	private final double tau;
	private final double timeStepSize;

	public RandomAlternativeVelocityChooser(Scenario sc) {
		this.tau = ((Sim2DConfigGroup)sc.getConfig().getModule("sim2d")).getTau();
		this.timeStepSize = ((Sim2DConfigGroup)sc.getConfig().getModule("sim2d")).getTimeStepSize();
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
//		GeometryFactory geofac = new GeometryFactory();
		
		double dv_max = agent.getDesiredVelocity() * this.tau * this.timeStepSize;
		double collPMin = dv_max;
		
		
		double penalty = Double.POSITIVE_INFINITY;
		double bestVx = 0;
		double bestVy = 0;
		double[] oldBest = agent.getOldBest();
		double candX = 0; //df[0]; //oldBest[0];
		double candY = 0; //df[1] ;//0; //oldBest[1];
		for (int i = 0; i < NUM_OF_CANDIDATES; i++){

			double vxNext = agent.getVx() + this.timeStepSize*(candX - agent.getVx())/this.tau;
			double vyNext = agent.getVy() + this.timeStepSize*(candY - agent.getVy())/this.tau;
		
//			double stopTime = Math.hypot(vxNext, vyNext)/0.5;
			double stopTime = Math.sqrt(vxNext*vxNext + vyNext*vyNext)*this.tau; 
			Coordinate candC = new Coordinate(c0.x + vxNext, c0.y + vyNext);
			
			//DEBUG
//			LineString ls = geofac.createLineString(new Coordinate[]{c0,candC});
			double tToColl = timeToCollision(vOs, c0, candC);
			
			if (!Algorithms.testForCollision(vOs, candC) ) {
				double testDist = candC.distance(c1);
//				GisDebugger.addGeometry(ls,"pen:"+(int)(100*testDist) + " time: NaN");
				
				if (testDist < penalty) {
					penalty = testDist;
					bestVx = candX;
					bestVy = candY;
				}
			} else {
				double colTime = tToColl; //timeToCollision(vOs, c0, candC);
				double testPen = candC.distance(c1) + W_I/colTime;// + 0.5*MatsimRandom.getRandom().nextDouble();
				testPen = Math.max(testPen, collPMin);
//				GisDebugger.addGeometry(ls,"pen:"+(int)(100*testPen) + " time:" + colTime);

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
//			double r1 = this.random.nextDouble();
//			final double dv_max = 1.34*0.04*2; //TODO no magic numbers use tau simStepSize ... instead
//			candX = df[0] + r1 * dv_max;
//			double r2 = this.random.nextDouble();
//			candY = df[1] + r2 * dv_max;
			double r1 = this.random.nextDouble();
			candX = df[0]*.75 + r1 * agent.getDesiredVelocity();
			double r2 = this.random.nextDouble();
			candY = df[1]*.75 + r2 * agent.getDesiredVelocity();
		}
		
		
//		double v = Math.sqrt(bestVx*bestVx + bestVy*bestVy);
//		if (v > 1.34) {
//			bestVx /= (v/1.34);
//			bestVy /= (v/1.34);
////			System.out.println("!!");
//		}
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
