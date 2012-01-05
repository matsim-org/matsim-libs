package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d_v2.random.XORShiftRandom;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;

public class RandomAlternativeVelocityChooser extends AlternativeVelocityChooser {

	private static final int NUM_OF_CANDIDATES = 20;
	private static final double MAX_V_COEFF = 1.25;
	private final XORShiftRandom random;


	public RandomAlternativeVelocityChooser() {
		this.random = new XORShiftRandom(MatsimRandom.getRandom().nextLong());
	}


	@Override
	public void chooseAlterantiveVelocity(List<VelocityObstacle> vOs, Coordinate c0, Coordinate c1, double[] df, Agent2D agent) {

		double penalty = Double.POSITIVE_INFINITY;
		double bestVx = 0;
		double bestVy = 0;
		double candX = df[0];
		double candY = df[1];
		for (int i = 0; i < NUM_OF_CANDIDATES; i++){

			Coordinate candC = new Coordinate(c0.x + candX, c0.y + candY);
			if (!Algorithms.testForCollision(vOs, candC)) {
				double testDist = candC.distance(c1);
				if (testDist < penalty) {
					penalty = testDist;
					bestVx = candX;
					bestVy = candY;
				}
			} else {
				double colTime = timeToCollision(vOs, c0, candC);
				double testPen = candC.distance(c1) + agent.wi/colTime;

				//FIXME use simStepTime instead of 0.04
				if (colTime > 0.12 && testPen < penalty) {
					penalty = testPen;
					bestVx = candX;
					bestVy = candY;
				}
			}
			candX = this.random.nextDouble() * agent.getDesiredVelocity() * MAX_V_COEFF;
			candY = this.random.nextDouble() * agent.getDesiredVelocity() * MAX_V_COEFF;
		}
		df[0] = bestVx;
		df[1] = bestVy;
	}

}
