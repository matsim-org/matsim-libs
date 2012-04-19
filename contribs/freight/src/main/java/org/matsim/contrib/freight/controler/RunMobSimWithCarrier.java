/*
 *  *********************************************************************** *
// *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package org.matsim.contrib.freight.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanReader;
import org.matsim.contrib.freight.carrier.CarrierPlanWriter;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategy;
import org.matsim.contrib.freight.replanning.MemorizeSelectedPlan;
import org.matsim.contrib.freight.replanning.PlanStrategyManager;
import org.matsim.contrib.freight.replanning.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.ReScheduleVehicles;
import org.matsim.contrib.freight.replanning.ScheduleVehicles;
import org.matsim.contrib.freight.replanning.SelectBestPlan;
import org.matsim.contrib.freight.vrp.DTWSolverFactory;
import org.matsim.contrib.freight.vrp.NetworkTransportCosts;
import org.matsim.contrib.freight.vrp.algorithms.rr.ChartListener;
import org.matsim.contrib.freight.vrp.basics.CarrierCostParams;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;


public class RunMobSimWithCarrier implements StartupListener, ShutdownListener, BeforeMobsimListener, AfterMobsimListener, ScoringListener, ReplanningListener, IterationEndsListener {

	public static class MyCarrierCostParams extends CarrierCostParams {

		public static double transportCost_per_second = 50.0/(60.0*60.0);
		
		public static double transportCost_per_meter = 1.0/1000.0;
		
		public static double waitingCost_per_second = 50.0/(60.0*60.0);
		
		public static double serviceCost_per_second = 50.0/(60.0*60.0);
		
		public static double cost_per_secondTooLate = 1000.0/(60.0*60.0);
		
		public static double cost_per_vehicle = 100.0;
		
		
		public MyCarrierCostParams() {
			super(transportCost_per_second, transportCost_per_meter, waitingCost_per_second,
					serviceCost_per_second, cost_per_secondTooLate, cost_per_vehicle);
			// TODO Auto-generated constructor stub
		}
		
	}
	
	static class MyTravelCosts implements TravelDisutility{

		private TravelTime travelTime;
		
		private double cost_per_m = new MyCarrierCostParams().transportCost_per_meter;
		
		private double cost_per_s = new MyCarrierCostParams().transportCost_per_second;
		
		public MyTravelCosts(TravelTime travelTime) {
			super();
			this.travelTime = travelTime;
		}


		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			double genCosts = link.getLength()*cost_per_m + travelTime.getLinkTravelTime(link, time)*cost_per_s;
//			double genCosts = travelTime.getLinkTravelTime(link, time)*cost_per_s;
			return genCosts;
		}
		
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return getLinkTravelDisutility(link, Time.UNDEFINED_TIME, null, null);
		}
	}
	
    private static Logger logger = Logger.getLogger(RunMobSimWithCarrier.class);

    private CarrierAgentTracker carrierAgentTracker;
    private String carrierFilename;
    private Carriers carriers;
   

    public RunMobSimWithCarrier(String carrierFilename) {
        this.carrierFilename = carrierFilename;
    }

    @Override
		public void notifyStartup(StartupEvent event) {
        carriers = new Carriers();
        new CarrierPlanReader(carriers).read(carrierFilename);
       
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		carrierAgentTracker = new CarrierAgentTracker(carriers, event.getControler().getNetwork());
//		carrierAgentTracker.getEventsManager().addHandler(new PickupAndDeliveryConsoleWriter());
        City2000WQSimFactory mobsimFactory = new City2000WQSimFactory(carrierAgentTracker);	
		event.getControler().setMobsimFactory(mobsimFactory);
		controler.getEvents().addHandler(carrierAgentTracker);
        carrierAgentTracker.createPlans();
	}

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        Controler controler = event.getControler();
        controler.getEvents().removeHandler(carrierAgentTracker);
    }

    @Override
	public void notifyScoring(ScoringEvent event) {
		carrierAgentTracker.scoreSelectedPlans();
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		CarrierPlanStrategy planStrat_reSchedule = new CarrierPlanStrategy();
		
		final LeastCostPathCalculator router = new FastDijkstraFactory().createPathCalculator(event.getControler().getScenario().getNetwork(), 
				new MyTravelCosts(event.getControler().getTravelTimeCalculator()) , event.getControler().getTravelTimeCalculator());
		
//		final LeastCostPathCalculator router = event.getControler().getLeastCostPathCalculatorFactory().createPathCalculator(event.getControler().getScenario().getNetwork(), event.getControler().createTravelCostCalculator(), event.getControler().getTravelTimeCalculator());
		
		Costs costs = new NetworkTransportCosts(router, new MyCarrierCostParams(), event.getControler().getNetwork(), event.getControler().getConfig().travelTimeCalculator().getTraveltimeBinSize());
		
		String filename = event.getControler().getControlerIO().getIterationPath(event.getIteration()) + "/" + event.getIteration() + ".vrp.png" ;
		ChartListener chartListener = new ChartListener();
		chartListener.setFilename(filename);
		
		ReScheduleVehicles vehicleReRouter = new ReScheduleVehicles(event.getControler().getNetwork(), costs);
//		vehicleReRouter.listeners.add(chartListener);
		
		planStrat_reSchedule.addModule(new MemorizeSelectedPlan());
		planStrat_reSchedule.addModule(vehicleReRouter);
		planStrat_reSchedule.addModule(new ReRouteVehicles(router, event.getControler().getNetwork()));
		
		ScheduleVehicles vehicleRouter = new ScheduleVehicles(event.getControler().getNetwork(), costs, new DTWSolverFactory());
		vehicleRouter.setRouter(router);
		
		CarrierPlanStrategy planStrat_schedule = new CarrierPlanStrategy();
		planStrat_schedule.addModule(new MemorizeSelectedPlan());
		planStrat_schedule.addModule(vehicleRouter);
		planStrat_schedule.addModule(new ReRouteVehicles(router, event.getControler().getNetwork()));
		
		CarrierPlanStrategy planStrat_bestPlan = new CarrierPlanStrategy();
		planStrat_bestPlan.addModule(new MemorizeSelectedPlan());
		planStrat_bestPlan.addModule(new SelectBestPlan());
		
		CarrierPlanStrategy planStrat_reRoutePlan = new CarrierPlanStrategy();
		planStrat_reRoutePlan.addModule(new MemorizeSelectedPlan());
		planStrat_reRoutePlan.addModule(new SelectBestPlan());
		planStrat_reRoutePlan.addModule(new ReRouteVehicles(router, event.getControler().getNetwork()));
		
		PlanStrategyManager<Carrier> stratManager = new PlanStrategyManager<Carrier>();
		stratManager.addStrategy(planStrat_reSchedule, 0.05);
//		stratManager.addStrategy(planStrat_schedule, 0.1);
		stratManager.addStrategy(planStrat_bestPlan, 0.9);
		stratManager.addStrategy(planStrat_reRoutePlan, 0.05);
		
		for(Carrier carrier : carriers.getCarriers().values()){
			if(carrier.getSelectedPlan() == null){
				logger.warn("carrier cannot replan since no selected plan is available");
				continue;
			}
			stratManager.nextStrategy().run(carrier);
		}
		
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String dir = event.getControler().getControlerIO().getIterationPath(event.getIteration());
		new CarrierPlanWriter(carriers.getCarriers().values()).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		
	}

}
