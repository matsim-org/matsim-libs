/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.agarwalamit.congestionPricing.testExamples;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV6;

/**
 * @author amit
 */

public class CongestionTestExamples {
	
	private final boolean useOTFVis = false;
	
	public static void main(String[] args) {
		new CongestionTestExamples().run();
	}
	
	private  void run() {
		// corridor network
		int numberOfPersonInPlan = 4;
		String testNetwork = "corridor";
		CorridorNetworkAndPlans corridorInputs = new CorridorNetworkAndPlans();
		corridorInputs.createNetwork();
		corridorInputs.createPopulation(numberOfPersonInPlan);
		Scenario sc = corridorInputs.getDesiredScenario();
		new CongestionTestExamples().getDelayDataForPricingImpls(sc,testNetwork);
		
		// merging network
		testNetwork = "merging";
		MergingNetworkAndPlans mergeInputs = new MergingNetworkAndPlans();
		mergeInputs.createNetwork();
		mergeInputs.createPopulation();
		sc = mergeInputs.getDesiredScenario();
		new CongestionTestExamples().getDelayDataForPricingImpls(sc,testNetwork);
		
		//diverging network 
		numberOfPersonInPlan = 6;
		testNetwork = "diverging";
		DivergingNetworkAndPlans divergeInputs = new DivergingNetworkAndPlans();
		divergeInputs.createNetwork();
		divergeInputs.createPopulation(numberOfPersonInPlan);
		sc=divergeInputs.getDesiredScenario();
		new CongestionTestExamples().getDelayDataForPricingImpls(sc,testNetwork);
	}
	
	public void getDelayDataForPricingImpls(Scenario sc, String networkExampleName){
		List<CongestionEvent> congestionEvents_v3 = getCongestionEvents("v3",sc);
		List<CongestionEvent> congestionEvents_v4 = getCongestionEvents("v4",sc); 
		List<CongestionEvent> congestionEvents_v6 = getCongestionEvents("v6",sc);
		
		System.out.println("v3");
		 SortedMap<String,Tuple<Double, Double>> tab_v3 = getId2CausedAndAffectedDelays(congestionEvents_v3,sc);
		 System.out.println("v4");
		 SortedMap<String,Tuple<Double, Double>> tab_v4 = getId2CausedAndAffectedDelays(congestionEvents_v4,sc);
		 System.out.println("v6");
		 SortedMap<String,Tuple<Double, Double>> tab_v6 = getId2CausedAndAffectedDelays(congestionEvents_v6, sc);
		 
		 BufferedWriter writer = IOUtils.getBufferedWriter("./output/comparison_v3Vsv4_"+networkExampleName+".txt");
		 try {
			 writer.write("personId \t delayCaused_v6 \t delayAffected_v6 \t delayCaused_v3 \t delayAffected_v3 \t delayCaused_v4 \t delayAffected_v4  \n");
			 for(String personId : tab_v3.keySet()){
				 writer.write(personId+"\t"+tab_v6.get(personId).getFirst()+"\t"+tab_v6.get(personId).getSecond()+"\t"+tab_v3.get(personId).getFirst()+"\t"+tab_v3.get(personId).getSecond()+"\t"+
						 tab_v4.get(personId).getFirst()+"\t"+tab_v4.get(personId).getSecond()+"\n");
			 }
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "+ e);
		}
	}
	
	private SortedMap<String,Tuple<Double, Double>> getId2CausedAndAffectedDelays(List<CongestionEvent> events, Scenario sc){
		SortedMap<String,Tuple<Double, Double>> id2CausingAffectedDelays = new TreeMap<String, Tuple<Double,Double>>();
		
		for(int i=1;i<=sc.getPopulation().getPersons().size();i++){
			Id<Person> id = Id.createPersonId(i);
			id2CausingAffectedDelays.put(id.toString(), new Tuple<Double, Double>(0., 0.));
		}
		
		for(CongestionEvent e : events){
			System.out.println(e.toString());
			Tuple<Double, Double> causingPerson_tup = id2CausingAffectedDelays.get(e.getCausingAgentId().toString());
			causingPerson_tup = new Tuple<Double, Double>(causingPerson_tup.getFirst()+e.getDelay(), causingPerson_tup.getSecond());
			id2CausingAffectedDelays.put(e.getCausingAgentId().toString(), causingPerson_tup);
			
			Tuple<Double, Double> affectedPerson_tup = id2CausingAffectedDelays.get(e.getAffectedAgentId().toString());
			affectedPerson_tup = new Tuple<Double, Double>(affectedPerson_tup.getFirst(), affectedPerson_tup.getSecond()+e.getDelay());
			id2CausingAffectedDelays.put(e.getAffectedAgentId().toString(), affectedPerson_tup);
		}
		return id2CausingAffectedDelays;
	}

	private List<CongestionEvent> getCongestionEvents (String congestionPricingImpl, Scenario sc) {
		sc.getConfig().qsim().setStuckTime(3600);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new printAllEvents());

		final List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();

		events.addHandler( new CongestionEventHandler() {

			@Override
			public void reset(int iteration) {				
			}

			@Override
			public void handleEvent(CongestionEvent event) {
				congestionEvents.add(event);
			}
		});
		
		if(congestionPricingImpl.equalsIgnoreCase("v3")) {
			events.addHandler(new CongestionHandlerImplV3(events, (ScenarioImpl)sc));
			Logger.getLogger(CongestionTestExamples.class).warn("The analysis table is generated using events and thus there are some delays unaccounted in implV3.");
		}
		else if(congestionPricingImpl.equalsIgnoreCase("v4")) events.addHandler(new CongestionHandlerImplV4(events, sc));
		else if(congestionPricingImpl.equalsIgnoreCase("v6")) events.addHandler(new CongestionHandlerImplV6(events, sc));

		QSim sim = createQSim(sc, events);
		sim.run();
		
		return congestionEvents;
	}

	private class printAllEvents implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {

		@Override
		public void reset(int iteration) {
			
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			System.out.println(event.toString());
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			System.out.println(event.toString());
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			System.out.println(event.toString());			
		}
		
	}
	

	private QSim createQSim (Scenario sc, EventsManager manager){
		QSim qSim1 = new QSim(sc, manager);
		ActivityEngine activityEngine = new ActivityEngine(manager, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim1);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, manager);
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(Double.POSITIVE_INFINITY);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);
		
		if ( useOTFVis ) {
			// otfvis configuration.  There is more you can do here than via file!
			final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
			otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
			//				otfVisConfig.setShowParking(true) ; // this does not really work

			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, manager, qSim);
			OTFClientLive.run(sc.getConfig(), server);
		}

		return qSim;
	}
}
