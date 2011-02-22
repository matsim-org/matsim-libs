package playground.mrieser.core.mobsim.fakes;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.mobsim.api.DriverAgent;
import playground.mrieser.core.mobsim.network.api.MobsimLink;

/**
 * @author mrieser
 */
public class FakeDriverAgent implements DriverAgent {
	@Override
	public Id getNextLinkId() {
		return null;
	}
	@Override
	public void notifyMoveToNextLink() {
	}
	@Override
	public double getNextActionOnCurrentLink() {
		return -1.0;
	}
	@Override
	public void handleNextAction(final MobsimLink link, final double time) {
	}
}
