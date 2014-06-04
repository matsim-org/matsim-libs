package playground.balac.freefloating.qsimParkingModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingChoice.carsharing.DummyParkingModuleWithFreeFloatingCarSharing;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingCoordInfo;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.SimStepParallelEventsManagerImpl;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.freefloating.config.FreeFloatingConfigGroup;


public class FreeFloatingQsimFactory implements MobsimFactory{

	private final static Logger log = Logger.getLogger(QSimFactory.class);

	private final Scenario scenario;
	private final Controler controler;
	private Collection<ParkingCoordInfo> freefloatingCars;

	private DummyParkingModuleWithFreeFloatingCarSharing parkingModule;
	public FreeFloatingQsimFactory(final Scenario scenario, final Controler controler) throws IOException {

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
		    
		   freefloatingCars = new ArrayList<ParkingCoordInfo>();
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
		    
		    	Link l = controler.getNetwork().getLinks().get(new IdImpl(arr[0]));
		    	
		    	
		    	
		    	for (int k = 0; k < Integer.parseInt(arr[1]); k++) {
		    		ParkingCoordInfo parkingInfo = new ParkingCoordInfo(new IdImpl(Integer.toString(i)), l.getCoord());
		    		freefloatingCars.add(parkingInfo);
		    		i++;
		    	}
		    	
		    	s = reader.readLine();
		    	
		    }
		    
		    parkingModule = new DummyParkingModuleWithFreeFloatingCarSharing(this.controler, freefloatingCars);
		    
		}
	
	}
	@Override
	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

		//TODO: create vehicle locations here
		final FreeFloatingConfigGroup configGroup = (FreeFloatingConfigGroup)
				scenario.getConfig().getModule( FreeFloatingConfigGroup.GROUP_NAME );
		
		parkingModule.resetForNewIterationStart();
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();
		QNetsimEngineFactory netsimEngFactory;
		if (numOfThreads > 1) {
			/*
			 * The SimStepParallelEventsManagerImpl can handle events from multiple threads.
			 * The (Parallel)EventsMangerImpl cannot, therefore it has to be wrapped into a
			 * SynchronizedEventsManagerImpl.
			 */
			if (!(eventsManager instanceof SimStepParallelEventsManagerImpl)) {
				eventsManager = new SynchronizedEventsManagerImpl(eventsManager);				
			}
			netsimEngFactory = new ParallelQNetsimEngineFactory();
			log.info("Using parallel QSim with " + numOfThreads + " threads.");
		} else {
			netsimEngFactory = new DefaultQNetsimEngineFactory();
		}
		QSim qSim = new QSim(sc, eventsManager);
		
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory = null;
		
		
		if (sc.getConfig().scenario().isUseTransit()) {
			agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			
			
			agentFactory = new FreeFloatingAgentFactory(qSim, scenario, controler, parkingModule);
			
			
		}
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		ParkFFVehicles parkSource = new ParkFFVehicles(sc.getPopulation(), agentFactory, qSim, freefloatingCars, this.scenario);

		qSim.addAgentSource(agentSource);
		qSim.addAgentSource(parkSource);

		
		
		return qSim;
	}
}
