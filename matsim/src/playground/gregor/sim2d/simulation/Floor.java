package playground.gregor.sim2d.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gregor.sim2d.controller.Sim2DConfig;
import playground.gregor.sim2d.simulation.Agent2D.AgentState;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;

public class Floor {



	private static final Logger log = Logger.getLogger(Floor.class);


	
	private final MultiPolygon structure;
	private final NetworkLayer graph;
	private final Set<Agent2D> agents = new HashSet<Agent2D>();
	private final Map<Agent2D,Force> agentForceMapping = new HashMap<Agent2D, Force>();
	private final StaticForceField staticForceField;
	
	private List<double[]> forceInfos;

	
	public Floor(MultiPolygon structure, NetworkLayer subnet, StaticForceField sff) {
		this.structure = structure;
		this.graph = subnet;
		this.staticForceField = sff;
	}

	public void move() {
		updateForces();
		moveAgents();
	}

	private void moveAgents() {
		for (Agent2D agent : this.agents) {
			Force force = this.agentForceMapping.get(agent);
			Coordinate oldPos = agent.getPosition();
			agent.setPosition(oldPos.x+force.getFx(),oldPos.y+force.getFy());
			force.setOldFx(force.getFx());
			force.setOldFy(force.getFy());
		}

	}

	private void updateForces() {

		//DEBUG
		this.forceInfos = new ArrayList<double[]>();


		for (Agent2D agent : this.agents) {		
			Force force = this.agentForceMapping.get(agent);
			if (agent.getState() == AgentState.ACTING) {
				//				Force force = this.agentForceMapping.get(agent);
				force.setFx(0.);
				force.setFy(0.);
			}
			else if  (agent.getState() == AgentState.MOVING) {
				//				updateAgentForce(agent);
				updateAgentInteractionForce(agent,force);
				updateAgentEnvForce(agent,force);
				updateDrivingForce(agent,force);

				validateForce(agent,force);
			}

			//DEBUG
			double [] tmp1 = {agent.getPosition().x,agent.getPosition().y,force.getFx(), force.getFy(),0.f};
			this.forceInfos.add(tmp1);
			double [] tmp2 = {agent.getPosition().x,agent.getPosition().y,force.interactionX, force.interactionY,1.f};
			this.forceInfos.add(tmp2);
			double [] tmp3 = {agent.getPosition().x,agent.getPosition().y,force.envX, force.envY,2.f};
			this.forceInfos.add(tmp3);
			double [] tmp4 = {agent.getPosition().x,agent.getPosition().y,force.driveX, force.driveY,3.f};
			this.forceInfos.add(tmp4);
		}
	}

	private void validateForce(Agent2D agent, Force force) {
		double norm= Math.sqrt(Math.pow(force.getFx(),2)  + Math.pow(force.getFy(),2));
		if (norm > agent.getDisiredVelocity()*Sim2DConfig.TIME_STEP_SIZE) {
			force.setFx(force.getFx() * ((agent.getDisiredVelocity()*Sim2DConfig.TIME_STEP_SIZE)/norm));
			force.setFy(force.getFy() * ((agent.getDisiredVelocity()*Sim2DConfig.TIME_STEP_SIZE)/norm));
		}
	}

	private void updateDrivingForce(Agent2D agent, Force force) {
		Link link = agent.getCurrentLink();
		if (agent.getPosition().distance(MGC.coord2Coordinate(link.getToNode().getCoord())) < 0.5) {
			link = agent.chooseNextLink();
		}
		if (link == null) {
			force.driveX = 0;
			force.driveY = 0;
		} else {
			Coordinate dest = MGC.coord2Coordinate(link.getToNode().getCoord());
			dest.x -= agent.getPosition().x;
			dest.y -= agent.getPosition().y;
			double norm = Math.sqrt(dest.x * dest.x + dest.y * dest.y);
			double scale = Sim2DConfig.TIME_STEP_SIZE*agent.getDisiredVelocity()/norm;
			if (scale > 1) {
				agent.chooseNextLink();
			}

			force.setFx(force.getFx() + (Sim2DConfig.TIME_STEP_SIZE*(dest.x*scale-force.getOldFx())/Sim2DConfig.tau));
			force.setFy(force.getFy() + (Sim2DConfig.TIME_STEP_SIZE*(dest.y*scale-force.getOldFy())/Sim2DConfig.tau));
			force.driveX = (dest.x*scale-force.getOldFx() )/Sim2DConfig.tau;
			force.driveY = (dest.y*scale-force.getOldFy())/Sim2DConfig.tau;

		}		

	}

	private void updateAgentEnvForce(Agent2D agent, Force force) {
		double x = 0;
		double y = 0;
		
		Force f = this.staticForceField.getForceWithin(agent.getPosition(), Sim2DConfig.STATIC_FORCE_RESOLUTION);
		
		if(f != null){
			 x = f.getFx();
			 y = f.getFy();
		}

		force.setFx(force.getFx() + (Sim2DConfig.Apw * x/agent.getWeight()));
		force.setFy(force.getFy() + (Sim2DConfig.Apw * y/agent.getWeight()));
		force.envX = Sim2DConfig.Apw * x/agent.getWeight();
		force.envY = Sim2DConfig.Apw * y/agent.getWeight();

	}

	private void updateAgentInteractionForce(Agent2D agent, Force force) {
		force.interactionX = 0;
		force.interactionY = 0;
		for (Agent2D other : this.agents) {
			if (other.equals(agent)) {
				continue;
			}
			double x = agent.getPosition().x - other.getPosition().x;
			double y = agent.getPosition().y - other.getPosition().y;
			double length = Math.sqrt(Math.pow(x,2)+Math.pow(y,2 ));
			if (length == 0 || length > Sim2DConfig.Bp) {
				continue;
			}
			double exp = Math.exp(length/Sim2DConfig.Bp);
			x *= exp/length;
			y *= exp/length;
			force.interactionX += x;
			force.interactionY += y;
		}

		force.setFx(force.getFx() + (Sim2DConfig.App * force.interactionX/agent.getWeight()));
		force.interactionX = Sim2DConfig.App * force.interactionX/agent.getWeight();
		force.setFy(force.getFy() + (Sim2DConfig.App * force.interactionY/agent.getWeight()));
		force.interactionY = Sim2DConfig.App * force.interactionY/agent.getWeight();

	}


	public void addAgent(Agent2D agent) {
		this.agents .add(agent);
		Force force = new Force();
		this.agentForceMapping.put(agent, force);
	}

	public Set<Agent2D> getAgents() {
		return this.agents;
	}



	//DEBUG
	public List<double[]> getForceInfos() {
		return this.forceInfos ;
	}

	public double getAgentVelocity(Agent2D agent) {
		Force force = this.agentForceMapping.get(agent);

		return Math.sqrt(Math.pow(force.getFx(), 2)+Math.pow(force.getFx(), 2));
	}
	
	public Force getAgentForce(Agent2D agent) {
		return this.agentForceMapping.get(agent);
	}
}
