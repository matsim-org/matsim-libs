package playground.gregor.sim2d_v2.simulation.floor;

import java.util.List;

import org.geotools.measure.Angle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import playground.gregor.sim2d_v2.controller.Sim2DConfig;
import playground.gregor.sim2d_v2.events.debug.ArrowEvent;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.Agent2D;

public class CollisionPredictionAgentInteractionModule implements
DynamicForceModule {


	private Floor floor;
	private Scenario2DImpl scenario;


	private final double quadUpdateInterval = 0.1;
	private double lastQuadUpdate = Double.NEGATIVE_INFINITY;
	private Quadtree coordsQuad;

	private final double EventHorizonTime = 10;
	private final GeometryFactory geofac = new GeometryFactory();

	//Helbing constants 
	public static final double Bi=0.34;
	public static final double Ai=250;

	public CollisionPredictionAgentInteractionModule(Floor floor, Scenario2DImpl scenario) {
		this.floor = floor;
		this.scenario = scenario;
	}

	@Override
	public void run(Agent2D agent) {
		double fx = 0;
		double fy = 0;

		double minX = agent.getPosition().x - Sim2DConfig.PNeighborhoddRange;
		double maxX = agent.getPosition().x + Sim2DConfig.PNeighborhoddRange;
		double minY = agent.getPosition().y - Sim2DConfig.PNeighborhoddRange;
		double maxY = agent.getPosition().y + Sim2DConfig.PNeighborhoddRange;
		Envelope e = new Envelope(minX, maxX, minY, maxY);
		List<Agent2D> l = this.coordsQuad.query(e);

		double t_i = getTi(l,agent);


		if (t_i == Double.POSITIVE_INFINITY) {
			return;
		}
		if (Sim2DConfig.DEBUG && agent.getPerson().getId().toString().equals("1")) {
			System.out.println("=========");
			System.out.println("t_i: " + t_i);
		}
		
		double v_i = Math.sqrt(agent.getVx()*agent.getVx() + agent.getVy()*agent.getVy());
		double stopTime = v_i / t_i;
		
		
		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}
			
			double dist = other.getPosition().distance(agent.getPosition());
			double term1 = Ai * stopTime * Math.exp(-dist/Bi);
			
			Vector v = getDistVector(agent,other,t_i);
			double projectedDist = Math.sqrt(v.x*v.x+v.y*v.y);
			

			fx += -term1 * v.x/projectedDist;
			fy += -term1 * v.y/projectedDist;
			
			
			
		}
		
		if (Sim2DConfig.DEBUG && agent.getPerson().getId().toString().equals("1") && Math.abs(fx) > 0.5 ) {
			System.out.println("======");
		}
		
		agent.getForce().incrementX(fx);
		agent.getForce().incrementY(fy);
		
	}

	private Vector getDistVector(Agent2D agent, Agent2D other, double t_i) {
		double projectedOtherX = other.getPosition().x + other.getVx() * t_i;
		double projectedOtherY = other.getPosition().y + other.getVy() * t_i;
		
		double projectedX = agent.getPosition().x + agent.getVx() * t_i;
		double projectedY = agent.getPosition().y + agent.getVy() * t_i;
		
		double dPrimeX_ij = projectedOtherX - projectedX;
		double dPrimeY_ij = projectedOtherY - projectedY;
		
		Vector v = new Vector();
		v.x = dPrimeX_ij;
		v.y = dPrimeY_ij;

		if (Sim2DConfig.DEBUG && agent.getPerson().getId().toString().equals("1")) {
			int o = Integer.parseInt(other.getPerson().getId().toString()) * 200;
			ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), new Coordinate(projectedX,projectedY,0), new Coordinate(projectedOtherX,projectedOtherY,0), 0.f,0.5f, 1.f, 50 + o);
			this.floor.getSim2D().getEventsManager().processEvent(arrow);
			int iii = 0;
			iii++;
		}
		
		return v;
	}

	private double getTi(List<Agent2D> l, Agent2D agent) {

		double t_i = Double.POSITIVE_INFINITY;



		for (Agent2D other : l) {
			if (other == agent) {
				continue;
			}
			double tmp = getTi(other,agent);
			if (tmp < t_i) {
				t_i = tmp;
			}
		}
		return t_i;
	}

	private double getTi(Agent2D other, Agent2D agent) {
		double relVx= other.getVx() - agent.getVx();
		double relVy = other.getVy() - agent.getVy();

		double relV = Math.sqrt(relVx*relVx + relVy * relVy);

		LineString ls = geofac.createLineString(new Coordinate[] {other.getPosition(),new Coordinate(other.getPosition().x+relVx*EventHorizonTime,other.getPosition().y+relVy*EventHorizonTime,0)});
		DistanceOp op =  new DistanceOp(ls, geofac.createPoint(agent.getPosition()));
		double ti = op.closestPoints()[0].distance(other.getPosition());
		double tanPhi = op.distance()/ti;
		double phi = Math.atan(tanPhi);
		if (phi > Math.PI) {
			phi = Math.PI*2 - phi;
		}

		if (phi > Math.PI/4){
			return Double.POSITIVE_INFINITY;
		}
		ti /= relV;
		if (Sim2DConfig.DEBUG && agent.getPerson().getId().toString().equals("1")) {
			int o = Integer.parseInt(other.getPerson().getId().toString()) * 100;
			ArrowEvent arrow = new ArrowEvent(agent.getPerson().getId(), other.getPosition(), ls.getEndPoint().getCoordinate(), 0.99f,0.5f, 0.f, 100 + o);
			this.floor.getSim2D().getEventsManager().processEvent(arrow);
			ArrowEvent arrow2 = new ArrowEvent(agent.getPerson().getId(),  agent.getPosition(), op.closestPoints()[0], 0.5f,0.99f, 0.f, 101 + o);
			this.floor.getSim2D().getEventsManager().processEvent(arrow2);
			phi = phi * (360/(Math.PI*2));
			System.out.println("other: " + other.getPerson().getId() + " dist:" + op.distance() + " time: " + ti + " phi:" + phi);
			System.out.println("===========");
		}
		return ti;

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
		this.coordsQuad = new Quadtree();
		for (Agent2D agent : this.floor.getAgents()) {
			Envelope e = new Envelope(agent.getPosition());
			this.coordsQuad.insert(e, agent);
		}

	}
	
	private static final class Vector {
		double x;
		
		double y;
	}

}
