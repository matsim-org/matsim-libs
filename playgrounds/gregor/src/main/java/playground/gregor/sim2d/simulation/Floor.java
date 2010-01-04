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
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gregor.sim2d.controller.Sim2DConfig;
import playground.gregor.sim2d.gisdebug.GisDebugger;
import playground.gregor.sim2d.simulation.Agent2D.AgentState;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class Floor {



	private static final Logger log = Logger.getLogger(Floor.class);



	
	private final MultiPolygon structure;
	private final NetworkLayer graph;
	private final Set<Agent2D> agents = new HashSet<Agent2D>();
	private final Map<Agent2D,Force> agentForceMapping = new HashMap<Agent2D, Force>();
	private final StaticForceField staticForceField;
	
	private List<double[]> forceInfos;
	
	//needed to generated "finish lines"
	@Deprecated
	private static final GeometryFactory geofac = new GeometryFactory();

	//needed to generated "finish lines"
	@Deprecated	
	private static final double COS_LEFT = Math.cos(Math.PI/2);
	//needed to generated "finish lines"
	@Deprecated
	private static final double SIN_LEFT = Math.sin(Math.PI/2);
	//needed to generated "finish lines"
	@Deprecated
	private static final double COS_RIGHT = Math.cos(-Math.PI/2);
	//needed to generated "finish lines"
	@Deprecated
	private static final double SIN_RIGHT = Math.sin(-Math.PI/2);
	
	
	private Map<Link,LineString> finishLines;
	private Map<Link,LineString> linksGeos;



	private Map<Link,Coordinate> drivingDirections;
	
	public Floor(MultiPolygon structure, NetworkLayer subnet, StaticForceField sff) {
		this.structure = structure;
		this.graph = subnet;
		this.staticForceField = sff;
		init();
	}

	//Here the perpendicular "finish lines" will be calculated
	//This should be done beforehand since "finsh lines" are part of the network  
	@Deprecated
	private void init() {
		this.finishLines = new HashMap<Link,LineString>();
		this.drivingDirections = new HashMap<Link, Coordinate>();
		this.linksGeos = new HashMap<Link,LineString>();
		for (Link link : this.graph.getLinks().values()) {
			LineString ls = getPerpendicularFinishLine(link, link.getToNode());
			this.finishLines.put(link, ls);
			Coordinate c = new Coordinate(link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX(),link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY());
			 double length = Math.sqrt(Math.pow(c.x, 2)+Math.pow(c.y, 2));
			 c.x /= length;
			 c.y /= length;
			 this.drivingDirections.put(link,c);
			 
			 LineString ls1 = this.geofac.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),MGC.coord2Coordinate(link.getToNode().getCoord())});
			this.linksGeos.put(link, ls1);
//				GisDebugger.addGeometry(ls1);
//				GisDebugger.dump("../../tmp/finishLine.shp");
		}
		
	}

	//needed to generated "finish lines"
	@Deprecated
	private LineString getPerpendicularFinishLine(Link link, Node node) {
		
		Coordinate c = new Coordinate(link.getFromNode().getCoord().getX()-link.getToNode().getCoord().getX(),link.getFromNode().getCoord().getY()-link.getToNode().getCoord().getY());
		double scale = 3/ Math.sqrt(Math.pow(c.x, 2)+Math.pow(c.y, 2));
		c.x *= scale;
		c.y *= scale;
		Coordinate c1 = new Coordinate(COS_LEFT*c.x + SIN_LEFT*c.y,-SIN_LEFT*c.x+COS_LEFT*c.y);
		c1.x += node.getCoord().getX();
		c1.y += node.getCoord().getY();
		Coordinate c2 = new Coordinate(COS_RIGHT*c.x + SIN_RIGHT*c.y,-SIN_RIGHT*c.x+COS_RIGHT*c.y);
		c2.x += node.getCoord().getX();
		c2.y += node.getCoord().getY();
		LineString ls = Floor.geofac.createLineString(new Coordinate[]{c1,c2});

		
//		LineString ls1 = this.geofac.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),MGC.coord2Coordinate(link.getToNode().getCoord())});
//		GisDebugger.addGeometry(ls1);
//		GisDebugger.addGeometry(ls);
//		GisDebugger.dump("../../tmp/finishLine.shp");
		return ls;
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
				updatePathForce(agent,force);
				
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
			double [] tmp5 = {agent.getPosition().x,agent.getPosition().y,force.pathX, force.pathY,4.f};
			this.forceInfos.add(tmp5);
		}
	}


	private void validateForce(Agent2D agent, Force force) {
		double norm= Math.sqrt(Math.pow(force.getFx(),2)  + Math.pow(force.getFy(),2));
		if (norm > agent.getDisiredVelocity()*Sim2DConfig.TIME_STEP_SIZE) {
			force.setFx(force.getFx() * ((agent.getDisiredVelocity()*Sim2DConfig.TIME_STEP_SIZE)/norm));
			force.setFy(force.getFy() * ((agent.getDisiredVelocity()*Sim2DConfig.TIME_STEP_SIZE)/norm));
		}
	}

	private void updatePathForce(Agent2D agent, Force force) {
		
		
		Link link = agent.getCurrentLink();
		LineString lsL = this.linksGeos.get(link);
		Coordinate c = this.drivingDirections.get(link);
		Point p = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y);
		double dist = p.distance(lsL);
		if (dist <= 0) {
			return;
		}
		
		double hypotenuse = agent.getPosition().distance(MGC.coord2Coordinate(link.getFromNode().getCoord()));
		double dist2 = Math.sqrt(Math.pow(hypotenuse, 2)-Math.pow(dist, 2));
		double deltaX = (link.getFromNode().getCoord().getX() + c.x * dist2) - agent.getPosition().x;
		double deltaY = (link.getFromNode().getCoord().getY() + c.y * dist2) - agent.getPosition().y;
		
		
		double f = Math.exp(dist/Sim2DConfig.Bpath);
		deltaX *= f/dist;
		deltaY *= f/dist;
		
		force.setFx(force.getFx() +Sim2DConfig.Apath * deltaX/agent.getWeight() );
		force.setFy(force.getFy() +Sim2DConfig.Apath * deltaY/agent.getWeight() );
		
//		log.info(agent.getId() + " deltaX: " + Sim2DConfig.Apath * deltaX/agent.getWeight() + " deltaY:" + Sim2DConfig.Apath * deltaY/agent.getWeight());
		
		//DEBUG		
		force.pathX = Sim2DConfig.Apath * deltaX/agent.getWeight();
		force.pathY = Sim2DConfig.Apath * deltaY/agent.getWeight();
	}
	private void updateDrivingForce(Agent2D agent, Force force) {
		Link link = agent.getCurrentLink();
		LineString ls = this.finishLines.get(link);
		Point p = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y);
		double dist = p.distance(ls);
		if (dist <= 0.2) {
			link = agent.chooseNextLink();
			if (link != null) {
				ls = this.finishLines.get(link);
				p = MGC.xy2Point(agent.getPosition().x, agent.getPosition().y);
				dist = p.distance(ls);
			}
		}
		if (link == null) {
			force.driveX = 0;
			force.driveY = 0;
		} else {
			
			
			
			Coordinate d = this.drivingDirections.get(link);
			double driveX = d.x;
			double driveY = d.y;
			
			driveX *= Sim2DConfig.TIME_STEP_SIZE*agent.getDisiredVelocity();
			driveY *= Sim2DConfig.TIME_STEP_SIZE*agent.getDisiredVelocity();
			
//			Coordinate dest = MGC.coord2Coordinate(link.getToNode().getCoord());
//			dest.x += agent.getPosition().x;
//			dest.y += agent.getPosition().y;
			
			
			if (dist < Sim2DConfig.TIME_STEP_SIZE*agent.getDisiredVelocity()) {
				agent.chooseNextLink();
			}

//			double rX = MatsimRandom.getRandom().nextDouble()/50-.05;
//			double rY = MatsimRandom.getRandom().nextDouble()/50-.05;
			
			force.setFx(force.getFx() + ((driveX-force.getOldFx())/Sim2DConfig.tau));
			force.setFy(force.getFy() + ((driveY-force.getOldFy())/Sim2DConfig.tau));
			force.driveX = (driveX-force.getOldFx() )/Sim2DConfig.tau;
			force.driveY = (driveY-force.getOldFy())/Sim2DConfig.tau;

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
			if (length <= 0.15 || length > Sim2DConfig.Bp) {
				continue;
			}
			double exp = Math.exp(Sim2DConfig.Bp/length);
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

	public void removeAgent(Agent2D agent) {
		this.agents.remove(agent);
		this.agentForceMapping.remove(agent);
	}
}
