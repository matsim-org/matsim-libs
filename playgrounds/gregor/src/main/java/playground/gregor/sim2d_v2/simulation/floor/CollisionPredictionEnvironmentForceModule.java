package playground.gregor.sim2d_v2.simulation.floor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import playground.gregor.sim2d_v2.controller.Sim2DConfig;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;

public class CollisionPredictionEnvironmentForceModule implements ForceModule {

	
	private Scenario2DImpl sc;
	private StaticForceField sff;
	
	private final double EventHorizonTime = 10;
	private final GeometryFactory geofac = new GeometryFactory();
	
	
	private static final double kappa = 2.4 * 100000;
	private static final double k = 1.2 * 100000;
	/**
	 * @param floor
	 * @param scenario
	 */
	public CollisionPredictionEnvironmentForceModule(Floor floor, Scenario2DImpl scenario) {
		this.sc = scenario;
		this.sff = this.sc.getStaticForceField();
	}
	
	@Override
	public void run(Agent2D agent) {
		double fx = 0;
		double fy = 0;
		ForceLocation fl = this.sff.getForceLocationWithin(agent.getPosition(), Sim2DConfig.STATIC_FORCE_RESOLUTION + 0.01);
		if (fl == null) {
			return;
		}
		EnvironmentDistances ed = fl.getEnvironmentDistances();
		
		double t_i = getTi(ed,agent);
		if (t_i == Double.POSITIVE_INFINITY) {
			return;
		}
		double v_i = Math.sqrt(agent.getVx()*agent.getVx() + agent.getVy()*agent.getVy());
		double stopTime = v_i / t_i;
		
		for (Coordinate c : ed.getObjects()) {
			double dist = c.distance(agent.getPosition());
			double term1 = CollisionPredictionAgentInteractionModule.Ai * stopTime * Math.exp(-dist/CollisionPredictionAgentInteractionModule.Bi);
			Vector v = getDistVector(agent,c,t_i);
			double projectedDist = Math.sqrt(v.x*v.x+v.y*v.y);
			

			
			double dx =(agent.getPosition().x - c.x) / dist;
			double dy =(agent.getPosition().y - c.y) / dist;

			double bounderyDist = Agent2D.AGENT_DIAMETER/4 - dist;
			double g = bounderyDist > 0 ? bounderyDist : 0;

			double tanDvx = (- agent.getVx()) * dx;
			double tanDvy = (- agent.getVy()) * dy;

			double tanX = tanDvx * -dx;
			double tanY = tanDvy * dy;
			
			
			fx += ((-term1  + k*g )* v.x/projectedDist) + kappa * g * tanX;;
			fy += ((-term1  + k*g )* v.y/projectedDist) + kappa * g * tanY;;
			
		}
		
		agent.getForce().incrementX(fx);
		agent.getForce().incrementY(fy);
		
	}

	private Vector getDistVector(Agent2D agent, Coordinate c, double t_i) {
		double projectedX = agent.getPosition().x + agent.getVx() * t_i;
		double projectedY = agent.getPosition().y + agent.getVy() * t_i;
		
		double dPrimeX_ij = c.x - projectedX;
		double dPrimeY_ij = c.y - projectedY;
		Vector v = new Vector();
		v.x = dPrimeX_ij;
		v.y = dPrimeY_ij;

		
		
		return v;
	}

	private double getTi(EnvironmentDistances ed, Agent2D agent) {
		double t_i = Double.POSITIVE_INFINITY;
		
		for (Coordinate c : ed.getObjects()) {
			double tmp = getTi(c,agent);
			if (tmp < t_i) {
				t_i = tmp;
			}
		}
		
		return t_i;
	}

	private double getTi(Coordinate c, Agent2D agent) {
		Coordinate c2 = new Coordinate(agent.getPosition().x + agent.getVx()*this.EventHorizonTime, agent.getPosition().y + agent.getVy()*this.EventHorizonTime,0);
		
		
		LineString ls = this.geofac.createLineString(new Coordinate [] {c,c2});
		
		DistanceOp op =  new DistanceOp(ls, geofac.createPoint(agent.getPosition()));
		double ti = op.closestPoints()[0].distance(c);
		double tanPhi = op.distance()/ti;
		double phi = Math.atan(tanPhi);
		if (phi > Math.PI) {
			phi = Math.PI*2 - phi;
		}

		if (phi > Math.PI/4){
			return Double.POSITIVE_INFINITY;
		}
		
		double v = Math.sqrt(agent.getVx()*agent.getVx()+ agent.getVy()*agent.getVy());
		ti /= v;
		
		
		return ti;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}
	
	private static final class Vector {
		double x;
		
		double y;
	}

}
