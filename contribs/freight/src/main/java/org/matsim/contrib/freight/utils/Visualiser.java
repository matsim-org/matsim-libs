package org.matsim.contrib.freight.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.contrib.freight.mobsim.FreightQSimFactory;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class Visualiser {
	
	private Config config;

	private Scenario scenario;

	private boolean live = true;
	
	public Visualiser(Config config, Scenario scenario){
		this.config = config;
		this.scenario = scenario;
	}
	
	public void visualise(Carriers carriers){
		sim(carriers);
	}
	
	private void sim(Carriers carriers) {
		QSimConfigGroup qSimConfigGroup = new QSimConfigGroup();
		config.addQSimConfigGroup(qSimConfigGroup);
		CarrierAgentTracker carrierAgentTracker = new CarrierAgentTracker(carriers, scenario.getNetwork(), new CarrierScoringFunctionFactory() {
			
			@Override
			public ScoringFunction createScoringFunction(Carrier carrier) {
				return getNoScoring();
			}
			
		});
		
		FreightQSimFactory mobsimFactory = new FreightQSimFactory(carrierAgentTracker);
		mobsimFactory.setWithinDayActivityReScheduling(true);
		
		EventsManager events = EventsUtils.createEventsManager();
		Mobsim mobsim = mobsimFactory.createMobsim(scenario, events);
		
		if(live){
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config, scenario, events, (QSim) mobsim);
			OTFClientLive.run(config, server);
		}
		mobsim.run();
	}
	
	private static ScoringFunction getNoScoring() {
		
		return new ScoringFunction(){

			@Override
			public void handleActivity(Activity activity) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void handleLeg(Leg leg) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void agentStuck(double time) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void addMoney(double amount) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void finish() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public double getScore() {
				// TODO Auto-generated method stub
				return 0;
			}

		

			@Override
			public void handleEvent(Event event) {
				// TODO Auto-generated method stub
				
			}
			
		};
	}

}
