package playground.andreas.intersection.sim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystems.BasicLane;
import org.matsim.basic.signalsystems.BasicLanesToLinkAssignment;
import org.matsim.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.events.LinkEnterEvent;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.mobsim.queuesim.QueueVehicle;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.mobsim.queuesim.VisData;
import org.matsim.mobsim.queuesim.QueueLane.AgentOnLink;
import org.matsim.signalsystems.CalculateAngle;
import org.matsim.utils.misc.Time;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;


public class QLink extends QueueLink {

	final private static Logger log = Logger.getLogger(QLink.class);
	private static int spaceCapWarningCount = 0;

	private PseudoLink originalLink = null;
	private double effectiveCelleSize;

	private ArrayList<PseudoLink> pseudoLinksList = new ArrayList<PseudoLink>();
	private ArrayList<PseudoLink> nodePseudoLinksList;

	private VisData visdata = this.new VisDataImpl();
	
	public QLink(final Link l, final QueueNetwork queueNetwork, final QueueNode toNode) {
		super(l, queueNetwork, toNode);

		this.effectiveCelleSize = queueNetwork.getNetworkLayer().getEffectiveCellSize();
		// Create first link (original link), no LaneId specified in file for this one 
		this.originalLink = new PseudoLink(this, true, null);

		// Configure it
		if(! this.originalLink.recalculatePseudoLinkProperties(0., this.getLink().getLength(), this.getLink().getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME), this.getLink().getFreespeed(Time.UNDEFINED_TIME), this.getLink().getFlowCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME),this.effectiveCelleSize)) {
			if ( spaceCapWarningCount <=10 ) {
				log.warn("Link " + this.getLink().getId() + " too small: enlarge spaceCap.  This is not fatal, but modifies the traffic flow dynamics.");
				if ( spaceCapWarningCount == 10 ) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++ ;
			}
		}

		this.pseudoLinksList.add(this.originalLink);
	}

	@Override
	public void addParking(QueueVehicle veh) {
		this.originalLink.addVehicle2ParkingQueue(veh);
	}

	@Override
	protected boolean moveLink(final double now) {
		for (PseudoLink pseudoLink : this.pseudoLinksList) {
			pseudoLink.movePseudoLink(now);
		}
		return true ;
	}

	@Override
	protected boolean bufferIsEmpty() {
		for (PseudoLink subLink : this.pseudoLinksList) {
			subLink.setThisTimeStepIsGreen(true);
		}
		
		if(this.nodePseudoLinksList == null){
//			this.originalLink.setThisTimeStepIsGreen(true);
			return this.originalLink.flowQueueIsEmpty();
		}
		
		for (PseudoLink subLink : this.nodePseudoLinksList) {
			if(!subLink.flowQueueIsEmpty()){
				return subLink.flowQueueIsEmpty();
			}
		}
		
		return true;
	}

	@Override
	public boolean hasSpace() {
		return this.originalLink.hasSpace();
	}

	@Override
	public void add(final QueueVehicle veh) {
		double now = SimulationTimer.getTime();
		activateLink();
		veh.getDriver().setCurrentLink(this.getLink());
		this.originalLink.addVehicle(veh);

		QueueSimulation.getEvents().processEvent(
				new LinkEnterEvent(now, veh.getDriver().getPerson(), this.getLink(), veh.getCurrentLeg()));
	}

	public List<PseudoLink> getNodePseudoLinks(){
		if ((this.nodePseudoLinksList == null) && (this.pseudoLinksList.size() == 1)){
			return this.pseudoLinksList;
		}
			return this.nodePseudoLinksList;
	}
	
	@Override
	public void addSignalGroupDefinition(BasicSignalGroupDefinition basicLightSignalGroupDefinition) {
		for (PseudoLink nodePseudoLink : this.nodePseudoLinksList) {
			nodePseudoLink.addLightSignalGroupDefinition(basicLightSignalGroupDefinition);
		}				
	}

	public void reconfigure(BasicLanesToLinkAssignment laneToLink, QueueNetwork queueNetwork) {

		if (this.getLink().getLength() < 60){
			log.warn("Link " + this.getLink().getId() + " is signalized by traffic light, but its length is less than 60m. This is not recommended." +
				"Recommended minimum link length is 45m for the signal lane and at least additional 15m space to store 2 vehicles at the original link.");
		}

		boolean firstNodeLinkInitialized = false;
		double averageSimulatedFlowCapacityPerLane_Veh_s = this.getSimulatedFlowCapacity() / this.getLink().getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME);

		for (BasicLane signalLane : laneToLink.getLanes()) {

			int numberOfLanes_ = signalLane.getNumberOfRepresentedLanes();
			double freeSpeed_m_s = this.getLink().getFreespeed(Time.UNDEFINED_TIME);
			PseudoLink newNodePseudoLink;

			if(!firstNodeLinkInitialized){
				newNodePseudoLink = new PseudoLink(this, false, signalLane.getId());
				this.originalLink.getToLinks().add(newNodePseudoLink);

				for (Id linkId : signalLane.getToLinkIds()) {
					newNodePseudoLink.addDestLink(queueNetwork.getQueueLink(linkId).getLink());
					this.originalLink.addDestLink(queueNetwork.getQueueLink(linkId).getLink());
				}

				newNodePseudoLink.recalculatePseudoLinkProperties(0, signalLane.getLength(), numberOfLanes_, freeSpeed_m_s, averageSimulatedFlowCapacityPerLane_Veh_s, this.effectiveCelleSize);
				this.originalLink.recalculatePseudoLinkProperties(signalLane.getLength(), this.getLink().getLength() - signalLane.getLength(), this.getLink().getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME), this.getLink().getFreespeed(Time.UNDEFINED_TIME), averageSimulatedFlowCapacityPerLane_Veh_s ,this.effectiveCelleSize);
				this.pseudoLinksList.add(newNodePseudoLink);
				firstNodeLinkInitialized = true;

			} else {
				// Now we have the original link and one extension pseudo Link
				// therefore add additional extension links for the rest of the outLinks

				// Check, if the new extension link is not in proximity of an old one's staring point
				if (signalLane.getLength() - this.originalLink.getMeterFromLinkEnd() > 15.0){
					// It is

					try {
						throw new Exception("Not Implemented yet: Every PseudoNode in 15m proximity of an old PseudoNode will be ajected to the old one");
					} catch (Exception e) {
						e.printStackTrace();
					}

					// Insert new one...
					// Fix Pointer...
					// Adjust SC, SQ...
					// Generate new NodePseudoLink
					// Fix Pointer...
					// Adjust SC, SQ...

				}else{
					// It is not
					// New NodePseudoLink will start at originalLink

					newNodePseudoLink = new PseudoLink(this, false, signalLane.getId());
					this.originalLink.getToLinks().add(newNodePseudoLink);

					for (Id linkId : signalLane.getToLinkIds()) {
						newNodePseudoLink.addDestLink(queueNetwork.getQueueLink(linkId).getLink());
						this.originalLink.addDestLink(queueNetwork.getQueueLink(linkId).getLink());
					}

					// Only need to fix properties of new link. Original link hasn't changed
					newNodePseudoLink.recalculatePseudoLinkProperties(0, signalLane.getLength(), numberOfLanes_, freeSpeed_m_s, averageSimulatedFlowCapacityPerLane_Veh_s ,this.effectiveCelleSize);
					this.pseudoLinksList.add(newNodePseudoLink);
				}
			}
		}

		findLayout();
		addUTurn();
		resortPseudoLinks();
	}

	private void resortPseudoLinks(){
		this.nodePseudoLinksList = new ArrayList<PseudoLink>();

		for (PseudoLink pseudoLink : this.pseudoLinksList) {
			if (pseudoLink.getMeterFromLinkEnd() == 0){
				this.nodePseudoLinksList.add(pseudoLink);
			}
		}

		Collections.sort(this.pseudoLinksList);
	}

	private void addUTurn(){
		for (Link outLink : this.getLink().getToNode().getOutLinks().values()) {

			if((outLink.getToNode().equals(this.getLink().getFromNode()))){
				PseudoLink tempPseudoLink = null;
				for (PseudoLink pseudoLink : this.pseudoLinksList) {

					if( (tempPseudoLink == null) || ((pseudoLink.visualizerLane == 1) && (pseudoLink.getMeterFromLinkEnd() == 0))){
						tempPseudoLink = pseudoLink;
						tempPseudoLink.addDestLink(outLink);
						this.originalLink.addDestLink(outLink);
					}
				}
			}
		}
	}

	private void findLayout(){
		SortedMap<Double, Link> result = CalculateAngle.getOutLinksSortedByAngle(this.getLink());

		for (PseudoLink pseudoLink : this.pseudoLinksList) {
			int lane = 1;
			for (Link link : result.values()) {
				if (pseudoLink.getDestLinks().contains(link)){
					pseudoLink.visualizerLane = lane;
					break;
				}
				lane++;
			}
		}
	}
	
	@Override
	public VisData getVisData() {
		return this.visdata;
	}
	
	class VisDataImpl implements VisData {

		public double getDisplayableSpaceCapValue() {
			throw new UnsupportedOperationException("Method not implemented in draft class");
		}

		public double getDisplayableTimeCapValue() {
			return 0.0;
		}

		public Collection<AgentOnLink> getDrawableCollection() {
			throw new UnsupportedOperationException("Method not implemented in draft class");
		}

		public Collection<PositionInfo> getVehiclePositions(final Collection<PositionInfo> positions) {
			String snapshotStyle = Gbl.getConfig().simulation().getSnapshotStyle();
			if ("queue".equals(snapshotStyle)) {
				getVehiclePositionsQueue(positions);
			} else {
				log.warn("The snapshotStyle \"" + snapshotStyle + "\" is not supported.");
			}
			return positions;
		}

		public void getVehiclePositionsEquil(Collection<PositionInfo> positions) {
			throw new UnsupportedOperationException("Method not implemented in draft class");
		}

		/**
		 * Calculates the positions of all vehicles on this link according to the
		 * queue-logic: Vehicles are placed on the link according to the ratio between
		 * the free-travel time and the time the vehicles are already on the link. If
		 * they could have left the link already (based on the time), the vehicles
		 * start to build a traffic-jam (queue) at the end of the link.
		 *
		 * @param positions
		 *          A collection where the calculated positions can be stored.
		 */
		public void getVehiclePositionsQueue(final Collection<PositionInfo> positions) {
			for (PseudoLink pseudoLink : pseudoLinksList) {
				pseudoLink.getVehPositions(positions);
			}
		}
		
	};



}