package playground.gregor.sim2d_v2.simulation.floor.forces.deliberative;

import java.util.Collection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v2.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.Algorithms;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CCWPolygon;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.ConfigurationSpaceObstacle;

public class VelocityObstacleForce implements DynamicForceModule{

	private Quadtree agentsQuad;
	private final PhysicalFloor floor;

	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;

	//Laemmel constant
	private static final double neighborhoodSensingRange = 5;

	public VelocityObstacleForce(PhysicalFloor floor) {
		this.floor = floor;
	}

	@Override
	public void run(Agent2D agent) {

		double minX = agent.getPosition().x - neighborhoodSensingRange;
		double maxX = agent.getPosition().x + neighborhoodSensingRange;
		double minY = agent.getPosition().y - neighborhoodSensingRange;
		double maxY = agent.getPosition().y + neighborhoodSensingRange;
		Envelope e = new Envelope(minX, maxX, minY, maxY);
		@SuppressWarnings("unchecked")
		Collection<Agent2D> l = this.agentsQuad.query(e);


		//		GeometryFactory geofac = new GeometryFactory();
		CCWPolygon aGeo = agent.getGeometry();
		//		LinearRing lr = geofac.createLinearRing(aGeo.getCCWRing());
		//		GisDebugger.addGeometry(lr);



		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}
			CCWPolygon oGeo = other.getGeometry();
			//			LinearRing lrO = geofac.createLinearRing(oGeo.getCCWRing());
			//			GisDebugger.addGeometry(lrO);

			Coordinate[] cso = ConfigurationSpaceObstacle.getCObstacle(oGeo, aGeo);
			//			LinearRing lrCso = geofac.createLinearRing(cso);
			//			GisDebugger.addGeometry(lrCso);

			int [] idx = Algorithms.getTangentIndices(agent.getPosition(),cso);
			Coordinate [] tan = {agent.getPosition(),cso[idx[0]],cso[idx[1]],agent.getPosition()};
			//			LinearRing ranR = geofac.createLinearRing(tan);
			//			GisDebugger.addGeometry(ranR);


		}


	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public void update(double time) {
		if (time >= this.lastQuadUpdate + this.quadUpdateInterval) {

			updateAgentQuadtree();

			this.lastQuadUpdate = time;
		}

	}

	@Override
	public void forceUpdate() {
		// TODO Auto-generated method stub

	}


	protected void updateAgentQuadtree() {
		this.agentsQuad = new Quadtree();
		for (Agent2D agent : this.floor.getAgents()) {
			Envelope e = new Envelope(agent.getPosition());
			this.agentsQuad.insert(e, agent);
		}

	}

}
