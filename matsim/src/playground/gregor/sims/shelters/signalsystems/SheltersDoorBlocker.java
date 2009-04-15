package playground.gregor.sims.shelters.signalsystems;

import org.matsim.core.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.core.basic.signalsystemsconfig.BasicAdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControlerImpl;

public class SheltersDoorBlocker extends AdaptiveSignalSystemControlerImpl {

	private ShelterInputCounter shelterInputCounter;

	public SheltersDoorBlocker(BasicAdaptiveSignalSystemControlInfo controlInfo) {
		super(controlInfo);
	}

	public boolean givenSignalGroupIsGreen(
			BasicSignalGroupDefinition signalGroup) {
		return this.shelterInputCounter.getShelterOfLinkHasSpace(signalGroup.getLinkRefId());
	}

	public void shelterInputCounter(ShelterInputCounter shelterInputCounter) {
		this.shelterInputCounter = shelterInputCounter;
		
	}

}
