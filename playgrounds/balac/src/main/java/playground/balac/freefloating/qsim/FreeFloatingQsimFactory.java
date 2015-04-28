package playground.balac.freefloating.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.utils.io.IOUtils;
import playground.balac.freefloating.config.FreeFloatingConfigGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;


public class FreeFloatingQsimFactory implements MobsimFactory{


	private final Scenario scenario;
	private final Controler controler;
	private final ArrayList<FreeFloatingStation> ffvehiclesLocation;

	public FreeFloatingQsimFactory(final Scenario scenario, final Controler controler) throws IOException {
		ffvehiclesLocation = new ArrayList<FreeFloatingStation>();

		this.scenario = scenario;
		this.controler = controler;
		readVehicleLocations();
	}
	public void readVehicleLocations() throws IOException {
		final FreeFloatingConfigGroup configGroupff = (FreeFloatingConfigGroup)
				scenario.getConfig().getModule( FreeFloatingConfigGroup.GROUP_NAME );
		
		
		BufferedReader reader;
		String s;
		
		
		if (configGroupff.useFeeFreeFloating()) {
		 reader = IOUtils.getBufferedReader(configGroupff.getvehiclelocations());
		    s = reader.readLine();
		    int i = 1;
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);

                Link l = controler.getScenario().getNetwork().getLinks().get(Id.create(arr[0], Link.class));
		    	
		    	ArrayList<String> vehIDs = new ArrayList<String>();
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[1]); k++) {
		    		vehIDs.add(Integer.toString(i));
		    		i++;
		    	}
		    	FreeFloatingStation f = new FreeFloatingStation(l, Integer.parseInt(arr[1]), vehIDs);
		    	
		    	ffvehiclesLocation.add(f);
		    	s = reader.readLine();
		    	
		    }	  
		}
	
	}
	@Override
	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

		//TODO: create vehicle locations here
		final FreeFloatingConfigGroup configGroup = (FreeFloatingConfigGroup)
				scenario.getConfig().getModule( FreeFloatingConfigGroup.GROUP_NAME );
		

		
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
		FreeFloatingVehiclesLocation ffvehiclesLocationqt = null;
		AgentFactory agentFactory = null;
		
		
		if (sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			
			
			try {
				ffvehiclesLocationqt = new FreeFloatingVehiclesLocation(controler, ffvehiclesLocation);
			
			
			
			agentFactory = new FreeFloatingAgentFactory(qSim, scenario, controler, ffvehiclesLocationqt);
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		ParkFFVehicles parkSource = new ParkFFVehicles(sc.getPopulation(), agentFactory, qSim, ffvehiclesLocationqt);

		qSim.addAgentSource(agentSource);
		qSim.addAgentSource(parkSource);

		
		
		return qSim;
	}
}
