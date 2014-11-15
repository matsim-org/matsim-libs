package playground.mzilske.teach.tasks2012;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;

public class MyAgent implements MobsimDriverAgent {

	private PersonDriverAgentImpl delegate;
	
	private final Netsim simulation;
	private static final Logger log = Logger.getLogger(MyAgent.class);

	private Id cachedNextLinkId = null;
	private Id cachedDestinationLinkId = null;
	private List<Id> RouteNodeIds = new ArrayList<Id>();
	private Id currentLinkId = null;
	private int currentNodeIdIndex = 0; // initialisation somewhere else?
	
	// for debug
	private int node_cnt = 0;
	
	public MyAgent(Person p, Plan unmodifiablePlan, QSim qSim) {
		// create a normal agent
		delegate = new PersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), qSim);
		this.simulation = qSim;
		
		if (unmodifiablePlan.getPlanElements().size() > 0) {
			Activity firstAct = (Activity) unmodifiablePlan.getPlanElements().get(0);	
			this.currentLinkId = firstAct.getLinkId();
		}
	}
	
	@Override
	public State getState() {
		return delegate.getState();
	}

	@Override
	public double getActivityEndTime() {
		return delegate.getActivityEndTime();
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		PlanElement pe = delegate.getCurrentPlanElement();
		
		if (pe instanceof Leg) { // when a leg ended 
			delegate.notifyArrivalOnLinkByNonNetworkMode(this.currentLinkId); // just a try
		}
		
		delegate.endActivityAndComputeNextState(now);
		
		pe = delegate.getCurrentPlanElement();
		
		if (pe instanceof Leg) {
			Leg leg = (Leg) pe;
			initializeLeg(leg);
		}
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		delegate.endLegAndComputeNextState(now);
		
		PlanElement pe = delegate.getCurrentPlanElement();
		if (pe instanceof Leg) {
			Leg leg = (Leg) pe;
			initializeLeg(leg);
		}
	}

	@Override
	public Double getExpectedTravelTime() {
		return delegate.getExpectedTravelTime();
	}

	@Override
	public String getMode() {
		return delegate.getMode();
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
		delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
		// but teleportation will not be used for the reactive agent anyway
	}

	@Override
	public Id getCurrentLinkId() {
		return delegate.getCurrentLinkId();
	}

	@Override
	public Id getDestinationLinkId() {
		return delegate.getDestinationLinkId();
	}

	@Override
	public Id getId() {
		return delegate.getId();
	}

	@Override
	public Id chooseNextLinkId() {
		// K. Semelka, 19.06.2012
		// missing: destination route handling
		
		if (this.cachedNextLinkId != null) {
			return this.cachedNextLinkId;
		}
		
		if (this.RouteNodeIds == null) {
			return null ;
		}

		if (this.currentNodeIdIndex >= this.RouteNodeIds.size()-1) {
			// we have no more information for the route, so the next link should be the destination link
			Link currentLink = this.simulation.getScenario().getNetwork().getLinks().get(this.currentLinkId);
			Link destinationLink = this.simulation.getScenario().getNetwork().getLinks().get(this.cachedDestinationLinkId);
			if (currentLink.getToNode().equals(destinationLink.getFromNode())) {
				this.cachedNextLinkId = destinationLink.getId();
				return this.cachedNextLinkId;
			}
			if (!(this.currentLinkId.equals(this.cachedDestinationLinkId))) {
				// there must be something wrong. Maybe the route is too short, or something else, we don't know...
				log.error("The vehicle with driver " + delegate.getPerson().getId() + ", currently on link " + this.currentLinkId.toString()
						+ ", is at the end of its route, but has not yet reached its destination link " + this.cachedDestinationLinkId.toString());
				//  personally, I would throw some kind of abort event here.  kai, aug'10
			}
			return null; // vehicle is at the end of its route
		}

		Link currentLink = this.simulation.getScenario().getNetwork().getLinks().get(this.currentLinkId);
		Node currentNode = currentLink.getToNode();
		
		for (Link l : currentNode.getOutLinks().values()) {
			if (l.getToNode().getId()==RouteNodeIds.get(currentNodeIdIndex+1)) {
				this.cachedNextLinkId = l.getId();
				return this.cachedNextLinkId;
			}
		}
		
		return null;
		
		
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		delegate.notifyMoveOverNode(newLinkId);
		node_cnt++;
		System.out.println(this.getId().toString()+" moved over a node! ("+node_cnt+")");
		
		this.currentNodeIdIndex++;
		this.currentLinkId = this.cachedNextLinkId;
		this.cachedNextLinkId = null; //reset cached nextLink
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		delegate.setVehicle(veh);
	}

	@Override
	public MobsimVehicle getVehicle() {
		return delegate.getVehicle();
	}

	@Override
	public Id getPlannedVehicleId() {
		return delegate.getPlannedVehicleId();
	}

	@Override
	public void setStateToAbort(double now) {
		delegate.setStateToAbort(now);
	}
	
	private void initializeLeg(Leg leg) {			
		Route route = leg.getRoute();
		this.cachedDestinationLinkId = route.getEndLinkId();
		
		TreeMap<Id,Double> arrTimes = new TreeMap<Id,Double>();
		Map<Id,Id> Pointers = new HashMap<Id,Id>();
		Set<Id> done = new HashSet<Id>();
 
		Link startLink = this.simulation.getScenario().getNetwork().getLinks().get(route.getStartLinkId());
		Link endLink = this.simulation.getScenario().getNetwork().getLinks().get(route.getEndLinkId());
		
		Node startNode = startLink.getToNode();
		Node endNode = endLink.getFromNode();
		
		Node currentNode = startNode;
		double now = leg.getDepartureTime();
		
		do {
			
			for (Link l : currentNode.getOutLinks().values()) {
				Id toNodeId = l.getToNode().getId();

				double tmpArrivalTime = now + l.getLength() / l.getFreespeed();

				if (arrTimes.containsKey(toNodeId)) {
					if ((arrTimes.get(toNodeId) > tmpArrivalTime) && (!done.contains(toNodeId))) {
						arrTimes.remove(toNodeId);
						arrTimes.put(toNodeId, tmpArrivalTime);

						if (Pointers.containsKey(toNodeId)) {
							Pointers.remove(toNodeId);
						}
						Pointers.put(toNodeId, currentNode.getId());

					}
				} else if (!done.contains(toNodeId)) {
					arrTimes.put(toNodeId, tmpArrivalTime);

					if (Pointers.containsKey(toNodeId)) {
						Pointers.remove(toNodeId);
					}
					Pointers.put(toNodeId, currentNode.getId());

				}
			}
			
			arrTimes.remove(currentNode.getId()); // set isDone of current node to false
			done.add(currentNode.getId());
			
			currentNode = this.simulation.getScenario().getNetwork().getNodes().get(arrTimes.firstKey());
			now = arrTimes.get(currentNode.getId());
			
		} while (!currentNode.equals(endNode));
		
		Id currentId = endNode.getId();
		RouteNodeIds.add(currentId);
		
		// get the route
		do {
			currentId = Pointers.get(currentId);
			RouteNodeIds.add(currentId);
		} while (!currentId.equals(startNode.getId()));
		
		Collections.reverse(RouteNodeIds);
		
		for (Id id : RouteNodeIds) {
			System.out.println(id.toString());
		}	
		
		// set the route according to the next leg
		this.currentNodeIdIndex = 0;
		this.cachedNextLinkId = null;
		return;
		
		}
	
	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		// The following is the old condition: Being at the end of the plan means you arrive anyways, no matter if you are on the right or wrong link.
		// kai, nov'14
		if ( this.chooseNextLinkId()==null ) {
			return true ;
		} else {
			return false ;
		}
	}

		
	}