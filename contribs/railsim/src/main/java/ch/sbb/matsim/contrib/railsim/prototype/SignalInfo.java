package ch.sbb.matsim.contrib.railsim.prototype;


import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Ihab Kaddoura
 */
public class SignalInfo {

	private final Link toLink;
	private final Link oppositeLink;
	private final List<Link> fromLink2fromLinkInfo;
	private final Map<SignalCondition, SignalGroupState> signalCondition2state;

	public enum SignalCondition {linkCapacity, nodeCapacity, nodeMinimumHeadway, oppositeDirection, conflictingOppositeLink}

	private Iterator<Link> iterator;
	private boolean considerInNextTimeStep = true;

	/**
	 * @param considerInNextTimeStep the considerInNextTimeStep to set
	 */
	public void setConsiderInNextTimeStep(boolean considerInNextTimeStep) {
		this.considerInNextTimeStep = considerInNextTimeStep;
	}

	public SignalInfo(Link toLink, Network network) {
		this.toLink = toLink;

		this.fromLink2fromLinkInfo = new ArrayList<>();

		for (Link inLink : toLink.getFromNode().getInLinks().values()) {
			if (inLink.getFromNode() == toLink.getToNode() && inLink.getToNode() == toLink.getFromNode()) {
				// skip the inverse link (assuming that turning around at nodes is not possible)
			} else {
				fromLink2fromLinkInfo.add(inLink);
			}
		}

		if (RailsimUtils.getOppositeDirectionLink(toLink, network) != null) {
			oppositeLink = network.getLinks().get(RailsimUtils.getOppositeDirectionLink(toLink, network));
		} else {
			oppositeLink = null;
		}

		iterator = this.fromLink2fromLinkInfo.iterator();

		this.signalCondition2state = new HashMap<>();

		// set initial conditions to green
		this.signalCondition2state.put(SignalCondition.linkCapacity, SignalGroupState.GREEN);
		this.signalCondition2state.put(SignalCondition.nodeCapacity, SignalGroupState.GREEN);
		this.signalCondition2state.put(SignalCondition.nodeMinimumHeadway, SignalGroupState.GREEN);
		this.signalCondition2state.put(SignalCondition.oppositeDirection, SignalGroupState.GREEN);
	}

	/**
	 * Changes the state (GREEN/RED) of a signal condition (e.g. capacity, minimum time)
	 *
	 * @param condition
	 * @param state
	 */
	public void changeCondition(SignalCondition condition, SignalGroupState state) {
		this.signalCondition2state.put(condition, state);
	}

	/**
	 * Returns true if all signal conditions are green, otherwise false.
	 */
	public boolean allConditionsGreen() {
		for (SignalGroupState state : this.signalCondition2state.values()) {
			if (state == SignalGroupState.RED) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the signalCondition2state
	 */
	public Map<SignalCondition, SignalGroupState> getSignalCondition2state() {
		return signalCondition2state;
	}

	/**
	 * @return the oppositeLink
	 */
	public Link getOppositeLink() {
		return oppositeLink;
	}

	/**
	 * @return the toLink
	 */
	public Link getToLink() {
		return toLink;
	}

	/**
	 * @return the fromLink2fromLinkInfo
	 */
	public List<Link> getFromLink2fromLinkInfo() {
		return fromLink2fromLinkInfo;
	}

	/**
	 * @return the next fromLink using an iterator which always starts from the beginning
	 */
	public Link getNextFromLink() {
		if (!iterator.hasNext()) {
			// set the iterator to the beginning
			iterator = this.fromLink2fromLinkInfo.iterator();
		}
		return iterator.next();
	}

	/**
	 * @return the considerInNextTimeStep
	 */
	public boolean isConsiderInNextTimeStep() {
		return considerInNextTimeStep;
	}

}
