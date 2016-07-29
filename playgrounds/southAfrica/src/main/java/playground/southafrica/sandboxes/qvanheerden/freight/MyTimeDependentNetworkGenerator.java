package playground.southafrica.sandboxes.qvanheerden.freight;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.southafrica.utilities.Header;

public class MyTimeDependentNetworkGenerator {

	public static void main(String[] args) {
		Header.printHeader(MyTimeDependentNetworkGenerator.class.toString(), args);
		
		String networkFile = args[0];
		String changeEventsOutputFile = args[1];
		
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
		sc.getConfig().network().setTimeVariantNetwork(true);
		
		NetworkChangeEventsWriter ncer = new NetworkChangeEventsWriter();
		double amStart = 0.;
		double amEnd = 5.;
		double pmStart = 15.;
		double pmEnd = 17.;
		
		Collection<NetworkChangeEvent> events = MyTimeDependentNetworkGenerator.getNetworkChangeEvents(sc, amStart, amEnd, pmStart, pmEnd); 
		ncer.write(changeEventsOutputFile, events);
		
		Header.printFooter();
	}
	
	public static Collection<NetworkChangeEvent> getNetworkChangeEvents(Scenario scenario, double amStart, double amEnd, double pmStart, double pmEnd) {
		Collection<NetworkChangeEvent> events = new ArrayList<NetworkChangeEvent>();
				
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			double speed = link.getFreespeed() ;
			double kmph = 1;
			final double threshold = kmph/3.6; //convert to m/s
			if ( speed > threshold ) {
				{//morning peak starts
					NetworkChangeEvent event = new NetworkChangeEvent(amStart*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold ));
					event.addLink(link);
					//((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					events.add(event);
				}
				{//morning peak ends
					NetworkChangeEvent event = new NetworkChangeEvent(amEnd*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  speed ));
					event.addLink(link);
					//((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					events.add(event);
				}
				{//afternoon peak starts
					NetworkChangeEvent event = new NetworkChangeEvent(pmStart*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  threshold ));
					event.addLink(link);
					//((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					events.add(event);
				}
				{//afternoon peak ends
					NetworkChangeEvent event = new NetworkChangeEvent(pmEnd*3600.) ;
					event.setFreespeedChange(new ChangeValue( ChangeType.ABSOLUTE_IN_SI_UNITS,  speed ));
					event.addLink(link);
					//((NetworkImpl)scenario.getNetwork()).addNetworkChangeEvent(event);
					events.add(event);
				}
			}
		}
		
		return events;
	}
	
}
