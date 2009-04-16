package playground.gregor.sims.shelters.signalsystems;

import org.matsim.core.basic.signalsystems.BasicSignalGroupDefinition;
import org.matsim.core.basic.signalsystemsconfig.BasicAdaptiveSignalSystemControlInfo;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControler;
import org.matsim.signalsystems.control.AdaptiveSignalSystemControlerImpl;

public class SheltersDoorBlockerController extends AdaptiveSignalSystemControlerImpl implements AdaptiveSignalSystemControler {

	private ShelterInputCounter shelterInputCounter;

	public SheltersDoorBlockerController(BasicAdaptiveSignalSystemControlInfo controlInfo) {
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
