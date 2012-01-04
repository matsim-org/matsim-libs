package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.List;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.VelocityObstacle;

import com.vividsolutions.jts.geom.Coordinate;

public interface AlternativeVelocityChooser {

	public void chooseAlterantiveVelocity(List<VelocityObstacle> vOs, Coordinate c0, Coordinate c1, double[] df, Agent2D agent);

}
