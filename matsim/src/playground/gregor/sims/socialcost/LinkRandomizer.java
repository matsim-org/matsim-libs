package playground.gregor.sims.socialcost;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.api.network.Link;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.TimeVariantLinkImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.utils.misc.Time;

public class LinkRandomizer implements BeforeMobsimListener{



	private static final double C = 0.1;
	private NetworkLayer network;

	public LinkRandomizer(NetworkLayer network) {
		this.network = network;
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		List<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>();
		double startTime = 3 * 3600;
		double endTime = 5 * 3600;



		for (Link link : this.network.getLinks().values()) {
			double detFlow = link.getFlowCapacity(Time.UNDEFINED_TIME);
			for (double time = startTime; time < endTime; time += 120) {
				NetworkChangeEvent e = new NetworkChangeEvent(time);
				e.addLink(link);
				double value =  detFlow + C * (MatsimRandom.getRandom().nextDouble() - .5) * detFlow; 
				ChangeValue c = new ChangeValue(ChangeType.ABSOLUTE,value);
				e.setFlowCapacityChange(c);
				events.add(e);
			}
		}
		this.network.setNetworkChangeEvents(events);
	}

}
