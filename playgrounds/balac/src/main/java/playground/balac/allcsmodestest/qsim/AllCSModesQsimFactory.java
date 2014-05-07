package playground.balac.allcsmodestest.qsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.carsharing.preprocess.membership.MyLinkUtils;
import playground.balac.freefloating.config.FreeFloatingConfigGroup;
import playground.balac.freefloating.qsim.FreeFloatingStation;
import playground.balac.freefloating.qsim.FreeFloatingVehiclesLocation;
import playground.balac.onewaycarsharingredisgned.qsim.OneWayCarsharingRDStation;
import playground.balac.onewaycarsharingredisgned.qsim.OneWayCarsharingRDVehicleLocation;
import playground.balac.twowaycarsharingredisigned.config.TwoWayCSConfigGroup;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSStation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSVehicleLocation;
import playground.balac.onewaycarsharingredisgned.config.OneWayCarsharingRDConfigGroup;

public class AllCSModesQsimFactory implements MobsimFactory{

	private final static Logger log = Logger.getLogger(QSimFactory.class);

	private final Scenario scenario;
	private final Controler controler;
	private final ArrayList<FreeFloatingStation> ffvehiclesLocation;
	private final ArrayList<OneWayCarsharingRDStation> owvehiclesLocation;
	private final ArrayList<TwoWayCSStation> twvehiclesLocation;
	public AllCSModesQsimFactory(final Scenario scenario, final Controler controler) throws IOException {
		
		this.scenario = scenario;
		this.controler = controler;
		ffvehiclesLocation = new ArrayList<FreeFloatingStation>();
		owvehiclesLocation = new ArrayList<OneWayCarsharingRDStation>();
		twvehiclesLocation = new ArrayList<TwoWayCSStation>();
		readVehicleLocations();
		//lets copy here locations and create another structure from this, might be faster
		
	}
	
	public void readVehicleLocations() throws IOException {
		final FreeFloatingConfigGroup configGroupff = (FreeFloatingConfigGroup)
				scenario.getConfig().getModule( FreeFloatingConfigGroup.GROUP_NAME );
		
		final OneWayCarsharingRDConfigGroup configGroupow = (OneWayCarsharingRDConfigGroup)
				scenario.getConfig().getModule( OneWayCarsharingRDConfigGroup.GROUP_NAME );
		
		final TwoWayCSConfigGroup configGrouptw = (TwoWayCSConfigGroup)
				scenario.getConfig().getModule( TwoWayCSConfigGroup.GROUP_NAME );
		BufferedReader reader;
		String s;
		if (configGroupff.useFeeFreeFloating()) {
		 reader = IOUtils.getBufferedReader(configGroupff.getvehiclelocations());
		    s = reader.readLine();
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
		    
		    	Link l = controler.getNetwork().getLinks().get(new IdImpl(arr[0]));
		    	
		    	FreeFloatingStation f = new FreeFloatingStation(l, Integer.parseInt(arr[1]));
		    	
		    	ffvehiclesLocation.add(f);
		    	s = reader.readLine();
		    	
		    }	  
		}
		if (configGroupow.useOneWayCarsharing()) {
		reader = IOUtils.getBufferedReader(configGroupow.getvehiclelocations());
		s = reader.readLine();
		    s = reader.readLine();
		    
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
		    
		    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
				Link l = MyLinkUtils.getClosestLink(controler.getNetwork(), coordStart);		    	
		    	
		    	OneWayCarsharingRDStation f = new OneWayCarsharingRDStation(l, Integer.parseInt(arr[6]));
		    	
		    	owvehiclesLocation.add(f);
		    	s = reader.readLine();
		    	
		    }	 
		}
		if (configGrouptw.useTwoWayCarsharing()) {
		    reader = IOUtils.getBufferedReader(configGrouptw.getvehiclelocations());
		    s = reader.readLine();
		    s = reader.readLine();
		    
		    while(s != null) {
		    	
		    	String[] arr = s.split("\t", -1);
		    
		    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
				Link l = MyLinkUtils.getClosestLink(controler.getNetwork(), coordStart);		    	
		    	
				TwoWayCSStation f = new TwoWayCSStation(l, Integer.parseInt(arr[6]));
		    	
				twvehiclesLocation.add(f);
		    	s = reader.readLine();
		    	
		    }	
		}
	}
	
	@Override
	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		if (conf.getNumberOfThreads() > 1) {
			/*
			 * The SimStepParallelEventsManagerImpl can handle events from multiple threads.
			 * The (Parallel)EventsMangerImpl cannot, therefore it has to be wrapped into a
			 * SynchronizedEventsManagerImpl.
			 */
			if (!(eventsManager instanceof SimStepParallelEventsManagerImpl)) {
				eventsManager = new SynchronizedEventsManagerImpl(eventsManager);				
			}
		}

		QSim qSim = new QSim(sc, eventsManager);
		
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		QNetsimEngineFactory netsimEngFactory;
		if (conf.getNumberOfThreads() > 1) {
			netsimEngFactory = new ParallelQNetsimEngineFactory();
			log.info("Using parallel QSim with " + conf.getNumberOfThreads() + " threads.");
		} else {
			netsimEngFactory = new DefaultQNetsimEngineFactory();
		}
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
			
			
			try {
				FreeFloatingVehiclesLocation ffvehiclesLocationqt = new FreeFloatingVehiclesLocation(controler, ffvehiclesLocation);
				OneWayCarsharingRDVehicleLocation owvehiclesLocationqt = new OneWayCarsharingRDVehicleLocation(controler, owvehiclesLocation);
				TwoWayCSVehicleLocation twvehiclesLocationqt = new TwoWayCSVehicleLocation(controler, twvehiclesLocation);
			
			agentFactory = new AllCSModesAgentFactory(qSim, scenario, controler, ffvehiclesLocationqt, owvehiclesLocationqt, twvehiclesLocationqt);
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}
	
	
	
}
