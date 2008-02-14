package playground.andreas.intersection.tl;

import org.matsim.network.Link;
import org.matsim.trafficlights.control.SignalSystemControler;
import org.matsim.trafficlights.data.SignalSystemConfiguration;

public class SignalSystemControlerImpl extends SignalSystemControler {
	
	private SignalSystemConfiguration signalSystemConfiguration;

	public SignalSystemControlerImpl(SignalSystemConfiguration signalSystemConfiguration) {
		this.signalSystemConfiguration = signalSystemConfiguration;
	}

	@Override
	public Link[] getGreenInLinks(double time) {
		// TODO Auto-generated method stub
		return null;
	}

}
