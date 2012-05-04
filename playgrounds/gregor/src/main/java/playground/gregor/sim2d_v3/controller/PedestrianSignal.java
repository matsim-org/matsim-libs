package playground.gregor.sim2d_v3.controller;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.mobsim.DefaultSignalizeableItem;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;

public class PedestrianSignal implements SignalizeableItem {

	private DefaultSignalizeableItem signalizedItem = null;
	private final Set<Id> outLinks;
	private final Id linkId;
	
	protected PedestrianSignal(Id linkId, Set<Id> outLinks) {
		this.outLinks = outLinks;
		this.linkId = linkId;
	}
	
	@Override
	public void setSignalized(boolean isSignalized) {
		this.signalizedItem = new DefaultSignalizeableItem(this.outLinks);

	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		this.signalizedItem.setSignalStateAllTurningMoves(state);
	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId) {
		if (!this.outLinks.contains(toLinkId)){
			throw new IllegalArgumentException("ToLink " + toLinkId + " is not reachable from current link Id");
		}
		this.signalizedItem.setSignalStateForTurningMove(state, toLinkId);
	}

	public boolean hasGreenForToLink(Id toLinkId){
		if (this.signalizedItem != null){
			return this.signalizedItem.isLinkGreenForToLink(toLinkId);
		}
		return true; //the lane/link is not signalized and thus always green
	}
	
	public Id getLinkId() {
		return this.linkId;
	}

	
}
