package playground.gregor.sim2d.simulation;


import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.Time;

import com.vividsolutions.jts.geom.Coordinate;

public class Agent2D  {

	public enum AgentState {MOVING,ACTING };
	
	private static final Logger log = Logger.getLogger(Agent2D.class);

	private final Person person;
	private final Plan currentPlan;

	int actLegPointer = 0;
	private Link currentLink;
	private LegImpl currentLeg;
	
	private List<Node> cacheRouteNodes;
	private int currentNodeIndex;

	private Coordinate position;

	private final Sim2D simulation;

	private AgentState state;
	
	private final double disieredVelocity = 1.0 + MatsimRandom.getRandom().nextDouble()/2-0.25;
//	private final double disieredVelocity = 1.34;
	private final double weight = 1000*(90 + 40*MatsimRandom.getRandom().nextDouble()-20);
	
	private Activity currentAct;

	private boolean departed = false;

	private Activity oldAct;


	public Agent2D(Person p, Sim2D sim2D) {
		this.person = p;
		this.currentPlan = p.getSelectedPlan();
		this.simulation = sim2D;
	}
	
	public Activity getAct() {
		return this.currentAct;
	}
	
	public Activity getOldAct() {
		return this.oldAct;
	}

	public void depart() {
		this.actLegPointer++;
		this.currentLeg = (LegImpl)this.currentPlan.getPlanElements().get(this.actLegPointer);
		this.oldAct = this.currentAct;
		this.currentAct = null;
		this.currentNodeIndex  = 0;
		this.cacheRouteNodes = null;
		this.state = AgentState.MOVING;
		this.departed  = true;
	}
	
	
	public Link getCurrentLink() {
		return this.currentLink;
	}

	public Coordinate getPosition() {
		return this.position;
	}

	public void setPosition(double x, double y) {
		this.position.x = x;
		this.position.y = y;
	}

	public double getDisiredVelocity() {
		return this.disieredVelocity;
	}
	
	public AgentState getState() {
		return this.state;
	}
	
	
	public Link chooseNextLink() {
		if (this.cacheRouteNodes == null) {
			this.cacheRouteNodes = ((NetworkRouteWRefs) this.currentLeg.getRoute()).getNodes();
			this.currentNodeIndex = 1;
		}
		if (this.currentNodeIndex >= this.cacheRouteNodes.size() ) {
			this.actLegPointer++;
			this.state = AgentState.ACTING;
			this.currentLeg = null;
			this.currentAct = (Activity) this.currentPlan.getPlanElements().get(this.actLegPointer);
			if (this.actLegPointer < this.currentPlan.getPlanElements().size()-1) {
				this.simulation.scheduleActivityEnd(this);
			} else {
				this.simulation.scheduleAgentRemove(this);
			}
			for (Link link :  this.currentLink.getToNode().getOutLinks().values()) {
				if (link.getId().equals(this.currentAct.getLinkId())) {
					this.currentLink = link;
				}
			}
			return null;
		}

		Node destNode = this.cacheRouteNodes.get(this.currentNodeIndex);

		for (Link link :  this.currentLink.getToNode().getOutLinks().values()) {
			if (link.getToNode() == destNode) {
				this.currentNodeIndex++;
				this.currentLink = link;
				return this.currentLink;
			}
		}
		log.warn(this + " [no link to next routenode found: routeindex= " + this.currentNodeIndex + " ]");
		return null;
	}


	public double getNextDepartureTime() {
		return this.currentAct.getEndTime();
	}

	public boolean initialize() {
		ActivityImpl firstAct = ((ActivityImpl)this.currentPlan.getPlanElements().get(this.actLegPointer));
		double departureTime = firstAct.getEndTime();
		this.currentLink = firstAct.getLink();
		this.position = new Coordinate(this.currentLink.getToNode().getCoord().getX(),this.currentLink.getToNode().getCoord().getY());
//		this.position = MGC.coord2Coordinate(firstAct.getCoord());
		if ((departureTime != Time.UNDEFINED_TIME) && (this.currentPlan.getPlanElements().size() > 1)) {
			this.currentAct = (Activity) this.currentPlan.getPlanElements().get(this.actLegPointer);
			this.simulation.scheduleActivityEnd(this);
			this.state = AgentState.ACTING;
			return true;
		}
		return false; // the agent has no leg, so nothing more to do
	}

	public Id getId() {
		return this.person.getId();
	}

	public double getWeight() {
		return weight;
	}

	public boolean departed() {
		if (this.departed) {
			this.departed = false;
			return true;
		}
		return false;
	}





}
