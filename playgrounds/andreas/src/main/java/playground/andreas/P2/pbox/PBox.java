/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.andreas.P2.pbox;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vehicles.VehiclesImpl;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.plan.PRouteProvider;
import playground.andreas.P2.plan.SimpleBackAndForthScheduleProvider;
import playground.andreas.P2.plan.SimpleCircleScheduleProvider;
import playground.andreas.P2.replanning.PStrategyManager;
import playground.andreas.P2.schedule.CreateStopsForAllCarLinks;
import playground.andreas.P2.scoring.ScoreContainer;
import playground.andreas.P2.scoring.ScorePlansHandler;
import playground.andreas.osmBB.extended.TransitScheduleImpl;

/**
 * Black box for paratransit
 * 
 * @author aneumann
 *
 */
public class PBox implements StartupListener, IterationStartsListener, ScoringListener{
	
	private final static Logger log = Logger.getLogger(PBox.class);
	
	private LinkedList<Cooperative> cooperatives;
	private int counter = 0;
	
	private final PConfigGroup pConfig;
	private PFranchise franchise;

	TransitSchedule pStopsOnly;
	TransitSchedule pTransitSchedule;
	
	TransitSchedule pTransitScheduleArchiv;
	
	private final ScorePlansHandler scorePlansHandler;
	private PStrategyManager strategyManager;
	private PRouteProvider routeProvider;
	
	public PBox(PConfigGroup pConfig) {
		this.pConfig = pConfig;		
		this.scorePlansHandler = new ScorePlansHandler(this.pConfig.getEarningsPerKilometerAndPassenger() / 1000.0, this.pConfig.getCostPerKilometer() / 1000.0);
		this.franchise = new PFranchise(this.pConfig.getUseFranchise());
		this.strategyManager = new PStrategyManager();
	}

	public void init(Controler controler){
		this.strategyManager.init(this.pConfig);
		
		this.scorePlansHandler.init(controler.getNetwork());
		controler.getEvents().addHandler(this.scorePlansHandler);
		
		// create stops
		this.pStopsOnly = CreateStopsForAllCarLinks.createStopsForAllCarLinks(controler.getNetwork(), this.pConfig);
		
		this.routeProvider = getRouteProvider(controler.getNetwork(), this.pConfig, this.pStopsOnly);	
		createCooperatives(this.pConfig.getNumberOfCooperatives());
		
		for (Cooperative cooperative : this.cooperatives) {
			cooperative.init(this.routeProvider, controler.getFirstIteration());
		}
	}	
	
	@Override
	public void notifyStartup(StartupEvent event) {
		// This is the first iteration - collect the transit schedules from all cooperatives
		this.pTransitSchedule = new TransitScheduleImpl(this.pStopsOnly.getFactory());
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		
		for (Cooperative cooperative : this.cooperatives) {
			this.pTransitSchedule.addTransitLine(cooperative.getCurrentTransitLine());
		}
		
		this.franchise.reset(this.pTransitSchedule);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// Replan all cooperatives
		if(this.pConfig.getUseAdaptiveNumberOfCooperatives()){
			// adapt the number of cooperatives
			adaptNumberOfCooperatives(event.getIteration());
		}		
		
		for (Cooperative cooperative : this.cooperatives) {
			cooperative.replan(this.strategyManager, event.getIteration());
		}
		
		this.pTransitSchedule = new TransitScheduleImpl(this.pStopsOnly.getFactory());
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		
		for (Cooperative cooperative : this.cooperatives) {
			this.pTransitSchedule.addTransitLine(cooperative.getCurrentTransitLine());
		}
		
		this.franchise.reset(this.pTransitSchedule);
		
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		TreeMap<Id, ScoreContainer> driverId2ScoreMap = this.scorePlansHandler.getDriverId2ScoreMap();
		for (Cooperative cooperative : this.cooperatives) {
			cooperative.score(driverId2ScoreMap);
		}
		
		this.pTransitSchedule = new TransitScheduleImpl(this.pStopsOnly.getFactory());
		for (TransitStopFacility stop : this.pStopsOnly.getFacilities().values()) {
			this.pTransitSchedule.addStopFacility(stop);
		}
		
		for (Cooperative cooperative : this.cooperatives) {
			this.pTransitSchedule.addTransitLine(cooperative.getCurrentTransitLine());
		}
		
		writeScheduleToFile(this.pTransitSchedule, event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "transitScheduleScored.xml.gz"));		
	}

	public TransitSchedule getpTransitSchedule() {
		return this.pTransitSchedule;
	}

	/**
	 * Create vehicles for each departure.
	 * 
	 * @return Vehicles of paratranit
	 */
	public Vehicles getVehicles(){		
		Vehicles vehicles = new VehiclesImpl();		
		VehiclesFactory vehFactory = vehicles.getFactory();
		VehicleType vehType = vehFactory.createVehicleType(new IdImpl("p"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(11)); // july 2011 the driver takes one seat
		capacity.setStandingRoom(Integer.valueOf(0));
		vehType.setCapacity(capacity);
		vehType.setAccessTime(2.0);
		vehType.setEgressTime(1.0);
		vehicles.getVehicleTypes().put(vehType.getId(), vehType);
	
		for (TransitLine line : this.pTransitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					Vehicle vehicle = vehFactory.createVehicle(departure.getVehicleId(), vehType);
					vehicles.getVehicles().put(vehicle.getId(), vehicle);
				}
			}
		}
		
		return vehicles;
	}

	public ScorePlansHandler getScorePlansHandler() {
		return scorePlansHandler;
	}

	public List<Cooperative> getCooperatives() {
		return cooperatives;
	}

	private void writeScheduleToFile(TransitSchedule schedule, String iterationFilename) {
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(schedule);
		writer.write(iterationFilename);		
	}

	private void addTransitRoutesToArchiv(int iteration) {
		
		if(pTransitScheduleArchiv == null){
			pTransitScheduleArchiv = this.pTransitSchedule;
		}
		
		for (TransitLine line : this.pTransitSchedule.getTransitLines().values()) {
			if(!this.pTransitScheduleArchiv.getTransitLines().containsKey(line.getId())){
				this.pTransitScheduleArchiv.addTransitLine(line);
			} else {
				for (TransitRoute route : line.getRoutes().values()) {
					pTransitScheduleArchiv.getTransitLines().get(line.getId()).addRoute(route);
				}
			}
		}		
	}

	private PRouteProvider getRouteProvider(NetworkImpl network, PConfigGroup pConfig, TransitSchedule pStopsOnly) {
		if(pConfig.getRouteProvider().equalsIgnoreCase(SimpleBackAndForthScheduleProvider.NAME)){
			return new SimpleBackAndForthScheduleProvider(pStopsOnly, network, 0);
		} else if(pConfig.getRouteProvider().equalsIgnoreCase(SimpleCircleScheduleProvider.NAME)){
			return new SimpleCircleScheduleProvider(pStopsOnly, network, 0);
		} else {
			log.error("There is no route provider specified. " + pConfig.getRouteProvider() + " unknown");
			return null;
		}
	}

	private void createCooperatives(int numberOfCooperatives) {
		this.cooperatives = new LinkedList<Cooperative>();
		for (int i = 0; i < numberOfCooperatives; i++) {
			this.counter++;
			BasicCooperative cooperative = new BasicCooperative(new IdImpl("p_" + this.counter), this.pConfig.getCostPerVehicle(), this.franchise);
			cooperatives.add(cooperative);
		}		
	}

	private void adaptNumberOfCooperatives(int iteration) {
		
		int numberOfProfitableCooperatives = 0;		
		for (Cooperative cooperative : this.cooperatives) {			
			List<PPlan> plans = cooperative.getAllPlans();			
			double tempSumScoreML = 0.0;
			for (PPlan plan : plans) {
				tempSumScoreML += plan.getScore();					
			}
			
			if(tempSumScoreML > 0){
				numberOfProfitableCooperatives++;
			}
		}
		
		if((double) numberOfProfitableCooperatives / (double) this.cooperatives.size() < this.pConfig.getShareOfCooperativesWithProfit()){
			// too few with profit, decrease by one company
			
			if(this.cooperatives.size() <= this.pConfig.getNumberOfCooperatives()){
				// do not remove any, we already have the minimum number of cooperatives
				return;
			}
			
			Cooperative cooperativeToRemove = null;
			double budgetOfThatCooperative = Double.MAX_VALUE;
			
			// find cooperative with lowest score
			for (Cooperative cooperative : this.cooperatives) {
				List<PPlan> plans = cooperative.getAllPlans();			
				double tempSumScoreML = 0.0;
				for (PPlan plan : plans) {
					tempSumScoreML += plan.getScore();					
				}
				
				if(tempSumScoreML < budgetOfThatCooperative){
					cooperativeToRemove = cooperative;
					budgetOfThatCooperative = tempSumScoreML;
				}
			}
			
			this.cooperatives.remove(cooperativeToRemove);
		} else {
			// too many with profit, there should be some market niche left, increase by one company
			this.counter++;
			BasicCooperative cooperative = new BasicCooperative(new IdImpl("p_" + this.counter), this.pConfig.getCostPerVehicle(), this.franchise);
			cooperative.init(this.routeProvider, iteration - 1);
			this.cooperatives.add(cooperative);
		}
		
		return;
	}

	
	
}
