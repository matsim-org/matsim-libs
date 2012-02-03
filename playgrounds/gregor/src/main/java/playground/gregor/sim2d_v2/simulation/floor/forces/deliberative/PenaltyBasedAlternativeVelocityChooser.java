package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;

public class PenaltyBasedAlternativeVelocityChooser extends AlternativeVelocityChooser {



	private final double w_i = 0.5;
	private static final double COS_PI_QUARTER = Math.cos(Math.PI/4);
	private static final double SIN_PI_QUARTER = Math.sin(Math.PI/4);

	private static final double COS_PI_QUARTER_RIGHT = Math.cos(-Math.PI/4);
	private static final double SIN_PI_QUARTER_RIGHT = Math.sin(-Math.PI/4);

	private static final double COS_PI_EIGHT = Math.cos(Math.PI/8);
	private static final double SIN_PI_EIGHT = Math.sin(Math.PI/8);

	private static final double COS_PI_SIXTEENTH = Math.cos(Math.PI/16);
	private static final double SIN_PI_SIXTEENTH = Math.sin(Math.PI/16);

	private static final double COS_PI_EIGHT_RIGHT = Math.cos(-Math.PI/8);
	private static final double SIN_PI_EIGHT_RIGHT = Math.sin(-Math.PI/8);

	private static final double COS_PI_SIXTEENTH_RIGHT = Math.cos(-Math.PI/16);
	private static final double SIN_PI_SIXTEENTH_RIGHT = Math.sin(-Math.PI/16);

	@Override
	public void chooseAlterantiveVelocity(List<? extends VelocityObstacle> vOs, Coordinate c0, Coordinate c1, double[] df, Agent2D agent) {

		double baseCollisionTime = timeToCollision(vOs, c0, c1);
		if (baseCollisionTime > this.timeHorizont) {
			return;
		}

		double minPenalty = this.w_i/baseCollisionTime;
		double retTime = 0;

		double dvx = df[0];
		double dvy = df[1];

		double retVx = dvx;
		double retVy = dvy;

		//45 deg to the left
		double nvx = COS_PI_QUARTER * dvx - SIN_PI_QUARTER * dvy;
		double nvy = SIN_PI_QUARTER * dvx + COS_PI_QUARTER * dvy;
		//		nvx *= 1.25;
		//		nvy *= 1.25;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		double time = timeToCollision(vOs, c0, c1);
		double diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		double penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retTime = time;
			retVx = nvx;
			retVy = nvy;
		}

		//22.5 deg to the left
		nvx = COS_PI_EIGHT * dvx - SIN_PI_EIGHT * dvy;
		nvy = SIN_PI_EIGHT * dvx + COS_PI_EIGHT * dvy;
		//		nvx *= 1.25;
		//		nvy *= 1.25;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retTime = time;
			retVx = nvx;
			retVy = nvy;
		}

		//11.125 deg to the left
		nvx = COS_PI_SIXTEENTH * dvx - SIN_PI_SIXTEENTH * dvy;
		nvy = SIN_PI_SIXTEENTH * dvx + COS_PI_SIXTEENTH * dvy;
		//		nvx *= 1.25;
		//		nvy *= 1.25;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retTime = time;
			retVx = nvx;
			retVy = nvy;
		}

		//		//90 deg to the left
		//		nvx = dvy;
		//		nvy = -dvx;
		//		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		//		time = timeToCollision(vOs, c0, c1);
		//		//		diff = 2*deltaVy;
		//		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		//		penalty = this.w_i / time + diff;
		//		if (penalty < minPenalty) {
		//			minPenalty = penalty;
		//			retVx = nvx;
		//			retVy = nvy;
		//			retTime =time;
		//		}



		//		//DEBUG
		//		GeometryFactory geofac = new GeometryFactory();
		//		LineString leftvA = geofac.createLineString(new Coordinate []{c0,c1});
		//		GisDebugger.addGeometry(leftvA,"left va: " + time);

		//45 deg to the left
		nvx = COS_PI_QUARTER_RIGHT * dvx - SIN_PI_QUARTER_RIGHT * dvy;
		nvy = SIN_PI_QUARTER_RIGHT * dvx + COS_PI_QUARTER_RIGHT * dvy;
		//		nvx *= 1.25;
		//		nvy *= 1.25;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retVx = nvx;
			retVy = nvy;
			retTime = time;
		}

		//22.5 deg to the left
		nvx = COS_PI_EIGHT_RIGHT * dvx - SIN_PI_EIGHT_RIGHT * dvy;
		nvy = SIN_PI_EIGHT_RIGHT * dvx + COS_PI_EIGHT_RIGHT * dvy;
		//		nvx *= 1.25;
		//		nvy *= 1.25;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retVx = nvx;
			retVy = nvy;
			retTime = time;
		}

		//22.5 deg to the left
		nvx = COS_PI_SIXTEENTH_RIGHT * dvx - SIN_PI_SIXTEENTH_RIGHT * dvy;
		nvy = SIN_PI_SIXTEENTH_RIGHT * dvx + COS_PI_SIXTEENTH_RIGHT * dvy;
		//		nvx *= 1.25;
		//		nvy *= 1.25;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retVx = nvx;
			retVy = nvy;
			retTime = time;
		}

		//		//90 deg to the right
		//		nvx = -dvy;
		//		nvy = dvx;
		//		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		//		time = timeToCollision(vOs, c0, c1);
		//		//		diff = 2*deltaVy;
		//		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		//		penalty = this.w_i / time + diff;
		//		if (penalty < minPenalty) {
		//			minPenalty = penalty;
		//			retVx = nvx;
		//			retVy = nvy;
		//			retTime = time;
		//		}

		//accel
		nvx = dvx*1.25;
		nvy = dvy*1.25;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retVx = nvx;
			retVy = nvy;
			retTime = time;
		}

		nvx = 0.8 * dvx;
		nvy = 0.8 * dvy;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retVx = nvx;
			retVy = nvy;
			retTime = time;
		}


		nvx = 0.5 * dvx;
		nvy = 0.5 * dvy;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retVx = nvx;
			retVy = nvy;
			retTime = time;
		}

		nvx = 0.25 * dvx;
		nvy = 0.25 * dvy;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retVx = nvx;
			retVy = nvy;
			retTime = time;
		}

		//break
		//		nvx = 0.5 * dvx;
		//		nvy = 0.5 * dvx;
		nvx = 0.1 * dvx;
		nvy = 0.1 * dvy;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty < minPenalty) {
			minPenalty = penalty;
			retVx = nvx;
			retVy = nvy;
			retTime = time;
		}

		//break
		//		nvx = 0.5 * dvx;
		//		nvy = 0.5 * dvx;
		nvx = 0.05 * dvx + (MatsimRandom.getRandom().nextDouble()-.5)/10;
		nvy = 0.05 * dvy + (MatsimRandom.getRandom().nextDouble()-.5)/10;
		c1 = new Coordinate(c0.x + nvx, c0.y + nvy);
		time = timeToCollision(vOs, c0, c1);
		//		diff = 2*deltaVy;
		diff = Math.sqrt((nvx-dvx)*(nvx-dvx)+(nvy-dvy)*(nvy-dvy));
		penalty = this.w_i / time + diff;
		if (penalty <= minPenalty) {
			minPenalty = penalty;
			retVx = nvx;
			retVy = nvy;
			retTime = time;
		}


		df[0] =  retVx;
		df[1] = retVy;


	}





}
