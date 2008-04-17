package playground.andreas.intersection.sim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.events.EventLinkEnter;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.LinkImpl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.trafficlights.data.SignalGroupDefinition;
import org.matsim.trafficlights.data.SignalLane;
import org.matsim.utils.misc.Time;

public class QLink extends LinkImpl {

	final private static Logger log = Logger.getLogger(QLink.class);
	private static int spaceCapWarningCount = 0;

	private PseudoLink originalLink = null;

	private ArrayList<PseudoLink> pseudoLinksList = new ArrayList<PseudoLink>();

	/** FreeLinkTravelTime */
	private double freeSpeedTT;
	private double effectiveCelleSize;

	public QLink(NetworkLayer network, String id, Node from, Node to, String length, String freespeed, String capacity, String permlanes, String origid, String type) {
		super(new Id(id), from, to, network, Double.parseDouble(length), Double.parseDouble(freespeed), Double.parseDouble(capacity), Double.parseDouble(permlanes));

		effectiveCelleSize = network.getEffectiveCellSize();

		freeSpeedTT = this.getLength() / this.getFreespeed(Time.UNDEFINED_TIME);
		// Original LinkErstellen
		originalLink = new PseudoLink(this, true);
		// Configurieren
		if(! originalLink.recalculatePseudoLinkProperties(0., this.getLength(), this.getLanesAsInt(), this.getFreespeed(Time.UNDEFINED_TIME), this.getFlowCapacity(),effectiveCelleSize)) {

			if ( spaceCapWarningCount <=10 ) {
				log.warn("Link " + this.getId() + " too small: enlarge spaceCap.  This is not fatal, but modifies the traffic flow dynamics.");
				if ( spaceCapWarningCount == 10 ) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++ ;
			}

		}

		pseudoLinksList.add(originalLink);
	}

	/** Adds a vehicle to the parkingQueue */
	public void addVehicle2ParkingQueue(QVehicle veh) {
		originalLink.addVehicle2ParkingQueue(veh);
	}

	public double getFreeSpeedTT() {
		return freeSpeedTT;
	}

	/** Called by QNetworkLayer */
	boolean moveLink(final double now) {

		for (PseudoLink pseudoLink : pseudoLinksList) {
			pseudoLink.movePseudoLink(now);
		}
		return true ;
	}

	public boolean hasSpace() {
		return originalLink.hasSpace();
	}

	public void add(final QVehicle veh) {
		veh.setCurrentLink(this);
		originalLink.addVehicle(veh);
		QSim.getEvents().processEvent(new EventLinkEnter(SimulationTimer.getTime(), veh.getDriverID(),
				veh.getCurrentLegNumber(), this.getId().toString(), veh.getDriver(), this));
	}

	public List<PseudoLink> getNodePseudoLinks(){

		List<PseudoLink> nodePseudoLinksList = new LinkedList<PseudoLink>();

		for (PseudoLink pseudoLink : pseudoLinksList) {
			if (pseudoLink.getMeterFromLinkEnd() == 0){
				nodePseudoLinksList.add(pseudoLink);
			}
		}
		return nodePseudoLinksList;
	}

	public void reconfigure(Set<SignalGroupDefinition> signalGroupDefinitions) {

		boolean firstNodeLinkInitialized = false;

		for (SignalGroupDefinition signalGroupDefinition : signalGroupDefinitions) {

			if(signalGroupDefinition.getLinkId().equals(id)){

				int numberOfLanes_ = 1;
				double freeSpeed_m_s = this.getFreespeed(Time.UNDEFINED_TIME);
				double flowCapacity_Veh_h = 2000.0 / ((NetworkLayer)this.getLayer()).getCapacityPeriod();

				PseudoLink newNodePseudoLink;

				for (Iterator iter = signalGroupDefinition.getToLanes().iterator(); iter.hasNext();) {
					SignalLane signalLane = (SignalLane) iter.next();

					if(!firstNodeLinkInitialized){

						newNodePseudoLink = new PseudoLink(this, false);

						newNodePseudoLink.setFromLink(originalLink);
						newNodePseudoLink.addDestLink(this.getToNode().getOutLinks().get(signalLane.getLinkId()));
						originalLink.getToLinks().add(newNodePseudoLink);
						originalLink.addDestLink(this.getToNode().getOutLinks().get(signalLane.getLinkId()));

						newNodePseudoLink.recalculatePseudoLinkProperties(0, signalLane.getLength(), numberOfLanes_, freeSpeed_m_s, flowCapacity_Veh_h,effectiveCelleSize);
						originalLink.recalculatePseudoLinkProperties(signalLane.getLength(), this.getLength() - signalLane.getLength(), this.getLanesAsInt(), this.getFreespeed(Time.UNDEFINED_TIME), this.getFlowCapacity(),effectiveCelleSize);

						pseudoLinksList.add(newNodePseudoLink);
						firstNodeLinkInitialized = true;

					} else {

						// Now we have the original link and one extension pseudo Link
						// therefore add additional extension links for the rest of the outLinks

						// Check, if the new extension link is in proximity of an old one's staring point
						if (signalLane.getLength() - originalLink.getMeterFromLinkEnd() > 15.0){
							// It is not

							System.err.println("Not Implemented yet: Every PseudoNode in 15m proximity of an old PseudoNode will be ajected to the old one");

							// Insert new one...
							// Fix Pointer...
							// Adjust SC, SQ...
							// Generate new NodePseudoLink
							// Fix Pointer...
							// Adjust SC, SQ...

						}else{
							// It is
							// New NodePseudoLink will start at originalLink

							newNodePseudoLink = new PseudoLink(this, false);

							newNodePseudoLink.setFromLink(originalLink);
							newNodePseudoLink.addDestLink(this.getToNode().getOutLinks().get(signalLane.getLinkId()));
							originalLink.getToLinks().add(newNodePseudoLink);
							originalLink.addDestLink(this.getToNode().getOutLinks().get(signalLane.getLinkId()));

							// Only need to fix properties of new link. Original link hasn't changed
							newNodePseudoLink.recalculatePseudoLinkProperties(0, signalLane.getLength(), numberOfLanes_, freeSpeed_m_s, flowCapacity_Veh_h,effectiveCelleSize);

							pseudoLinksList.add(newNodePseudoLink);

						}
					}
				}
			}
		}
	}

}
