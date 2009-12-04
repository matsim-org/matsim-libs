package playground.gregor.sims.socialcostII;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.utils.misc.Time;

public class LinkFlowCapRandomizer implements BeforeMobsimListener{



	private double C;
	private NetworkImpl network;
	private final double increment;

	public LinkFlowCapRandomizer(NetworkImpl network, double c, double increment) {
		this.network = network;
		this.C = c;
		this.increment = increment;
	}

	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		List<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>();
		double startTime = 0 * 3600;
		double endTime = 2 * 3600;

		for (LinkImpl link : this.network.getLinks().values()) {
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
		this.C += this.increment;
	}


}
