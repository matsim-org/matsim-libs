package playground.balac.twowaycarsharingredisigned.qsim;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;


public class TwoWayCSQsimFactory implements MobsimFactory{


	private final Scenario scenario;
	private final Controler controler;
	private final ArrayList<TwoWayCSStation> twvehiclesLocation;

	public TwoWayCSQsimFactory(final Scenario scenario, final Controler controler) throws IOException {
		
		this.scenario = scenario;
		this.controler = controler;
		this.twvehiclesLocation = new ArrayList<TwoWayCSStation>();
		readVehicleLocations();
	}
	public void readVehicleLocations() throws IOException {
		
		final TwoWayCSConfigGroup configGrouptw = (TwoWayCSConfigGroup)
				scenario.getConfig().getModule( TwoWayCSConfigGroup.GROUP_NAME );
		BufferedReader reader;
		String s;

        LinkUtils linkUtils = new LinkUtils(controler.getScenario().getNetwork());
		
	
		if (configGrouptw.useTwoWayCarsharing()) {
		    reader = IOUtils.getBufferedReader(configGrouptw.getvehiclelocations());
		    s = reader.readLine();
		    s = reader.readLine();
		    int i = 1;
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
		    
		    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
		    	Link l = linkUtils.getClosestLink(coordStart);			    	
				ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
				TwoWayCSStation f = new TwoWayCSStation(l, Integer.parseInt(arr[6]), vehIDs);
		    	
				twvehiclesLocation.add(f);
		    	s = reader.readLine();
		    	
		    }	
		}
	}
	@Override
	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

		//TODO: create vehicle locations here
		
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		QSim qSim = new QSim(sc, eventsManager);
		
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim);
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);

		AgentFactory agentFactory = null;
		TwoWayCSVehicleLocation twvehiclesLocationqt = null;

		
		if (sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			
				twvehiclesLocationqt = new TwoWayCSVehicleLocation(scenario, twvehiclesLocation);
				agentFactory = new TwoWayCSAgentFactory(qSim, scenario, controler, twvehiclesLocationqt);
			
		}
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		ParkTWVehicles parkSource = new ParkTWVehicles(sc.getPopulation(), agentFactory, qSim, twvehiclesLocationqt);
		qSim.addAgentSource(agentSource);
		qSim.addAgentSource(parkSource);
		
		return qSim;
	}
	
	private class LinkUtils {
		
		Network network;
		public LinkUtils(Network network) {
			
			this.network = network;		}
		
		public LinkImpl getClosestLink(Coord coord) {
			
			double distance = (1.0D / 0.0D);
		    Id<Link> closestLinkId = Id.create(0L, Link.class);
		    for (Link link : network.getLinks().values()) {
		      LinkImpl mylink = (LinkImpl)link;
		      Double newDistance = Double.valueOf(mylink.calcDistance(coord));
		      if (newDistance.doubleValue() < distance) {
		        distance = newDistance.doubleValue();
		        closestLinkId = link.getId();
		      }

		    }

		    return (LinkImpl)network.getLinks().get(closestLinkId);
			
			
		}
	}
}
