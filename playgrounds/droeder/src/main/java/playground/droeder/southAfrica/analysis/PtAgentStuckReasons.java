/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.southAfrica.analysis;


/**
 * @author droeder
 *
 */
class PtAgentStuckReasons {//extends AbstractAnalyisModule {
//
//	@SuppressWarnings("unused")
//	private static final Logger log = Logger
//			.getLogger(PtAgentStuckReasons.class);
//	private PtAGentStuckReasonsHandler handler;
//	private Population population;
//	private TransitSchedule schedule;
//
//	public PtAgentStuckReasons(Scenario sc) {
//		super(PtAgentStuckReasons.class.getSimpleName());
//		this.handler = new PtAGentStuckReasonsHandler(sc.getTransitSchedule());
//		this.population = sc.getPopulation();
//		this.schedule = sc.getTransitSchedule();
//	}
//
//	@Override
//	public List<EventHandler> getEventHandler() {
//		List<EventHandler> handler = new ArrayList<EventHandler>();
//		handler.add(this.handler);
//		return handler;
//	}
//
//	@Override
//	public void preProcessData() {
//
//	}
//
//	@Override
//	public void postProcessData() {
//		Person p;
//		ExperimentalTransitRoute route;
//		for(Entry<Id, WaitingPerson> e : this.handler.getStuckingPersons().entrySet()){
//			p = this.population.getPersons().get(e.getKey());
//			for(PlanElement pe: p.getSelectedPlan().getPlanElements()){
//				if(pe instanceof Leg){
//					if(((Leg) pe).getRoute() instanceof ExperimentalTransitRoute){
//						route = (ExperimentalTransitRoute) ((Leg) pe).getRoute();
//						if(this.schedule.getTransitLines().containsKey(route.getLineId())){
//							if(!this.schedule.getTransitLines().get(route.getLineId()).getRoutes().containsKey(route.getRouteId())){
//								e.getValue().transitRouteNotExisting(route.getRouteId());
//								System.out.println("1: " + route.getRouteId());
//								break;
//							}
//							System.out.println("3: " + route.getRouteId());
//						}else{
//							System.out.println("2: " + route.getRouteId());
//							e.getValue().transitRouteNotExisting(route.getRouteId());
//							break;
//						}
//					}else{
//						System.out.println(((Leg) pe).getRoute().getClass().getCanonicalName());
//					}
//				}
//			}
//		}
//	}
//
//	@Override
//	public void writeResults(String outputFolder) {
//		BufferedWriter w = IOUtils.getBufferedWriter(outputFolder + "ptStuckReasons.csv");
//		try {
//			w.write("id;time;waitingAt;goingTo;possibleVehiclesPassed;stuckRoute;\n");
//			for(Entry<Id, WaitingPerson> e : this.handler.getStuckingPersons().entrySet()){
//				w.write(e.getValue().toString() + "\n");
//			}
//			w.flush();
//			w.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//	}
//	
//	private class PtAGentStuckReasonsHandler implements
//												AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler,
//												PersonEntersVehicleEventHandler,
//												VehicleArrivesAtFacilityEventHandler,
//												PersonStuckEventHandler
//												 {
//
//		private Map<Id, WaitingPerson> personsWaiting;
//		private TransitSchedule schedule;
//		private List<Id> driver;
//		private Map<Id, MyVehicle> vehicles;
//		private Map<Id, WaitingPerson>  stuck;
//
//		/**
//		 * @param schedule
//		 */
//		public PtAGentStuckReasonsHandler(TransitSchedule schedule) {
//			this.schedule = schedule;
//			this.driver = new ArrayList<Id>(); 
//			this.vehicles = new HashMap<Id, MyVehicle>();
//			this.personsWaiting = new HashMap<Id,WaitingPerson>();
//			this.stuck = new HashMap<Id, WaitingPerson>();
//		}
//
//		/**
//		 * @return
//		 */
//		public Map<Id, WaitingPerson> getStuckingPersons() {
//			return this.stuck;
//		}
//
//		@Override
//		public void reset(int iteration) {
//			
//		}
//
//		@Override
//		public void handleEvent(VehicleArrivesAtFacilityEvent event) {
//			MyVehicle v = this.vehicles.get(event.getVehicleId());
//			List<Id> stops2come = v.getStops2come(event.getFacilityId());
//			for(WaitingPerson p: this.personsWaiting.values()){
//				p.offerEnter(stops2come, event.getFacilityId());
//			}
//		}
//
//		@Override
//		public void handleEvent(PersonEntersVehicleEvent event) {
//			this.personsWaiting.remove(event.getDriverId());
//		}
//
//		@Override
//		public void handleEvent(TransitDriverStartsEvent event) {
//			this.driver.add(event.getDriverId());
//			this.vehicles.put(event.getVehicleId(), new MyVehicle(event, 
//					this.schedule.getTransitLines().get(event.getTransitLineId()).
//					getRoutes().get(event.getTransitRouteId())));
//		}
//		
//		private class MyVehicle{
//
//			private List<TransitRouteStop> stops;
//
//			/**
//			 * @param event
//			 * @param transitRoute
//			 */
//			public MyVehicle(TransitDriverStartsEvent event, TransitRoute transitRoute) {
//				this.stops = transitRoute.getStops();
//			}
//			
//			public List<Id> getStops2come(Id stopId){
//				List<Id> stops2come = new ArrayList<Id>();
//				boolean add = false;
//				for(TransitRouteStop s: this.stops){
//					if(add){
//						stops2come.add(s.getStopFacility().getId());
//					}else if(s.getStopFacility().getId().equals(stopId)){
//						stops2come.add(s.getStopFacility().getId());
//						add = true;
//					}
//				}
//				return stops2come;
//			}
//			
//		}
//
//		@Override
//		public void handleEvent(AgentWaitingForPtEvent event) {
//			// store persons waiting for pt
//			this.personsWaiting.put(event.getDriverId(),	new WaitingPerson(event));
//		}
//		
//
//		@Override
//		public void handleEvent(PersonStuckEvent event) {
//			if(this.personsWaiting.containsKey(event.getDriverId())){
//				this.stuck.put(event.getDriverId(), this.personsWaiting.remove(event.getDriverId()));
//			}
//		}
//
//	}
//	
//	class WaitingPerson{
//		
//		private Id stopToGo;
//		private int possibleVehiclePassedButNotEntered;
//		private Id waitingAt;
//		private double time;
//		private Id agentId;
//		private String routeNotExisting;
//
//		WaitingPerson(AgentWaitingForPtEvent event){
//			this.stopToGo = event.getDestinationStopId();
//			this.waitingAt =  event.getWaitingAtStopId();
//			this.time = event.getTime();
//			this.agentId = event.getDriverId();
//		}
//		
//		/**
//		 * @param routeId
//		 */
//		public void transitRouteNotExisting(Id routeId) {
//			this.routeNotExisting = routeId.toString();
//		}
//
//		/**
//		 * @param stops2come
//		 * @param facilityId
//		 */
//		public void offerEnter(List<Id> stops2come, Id facilityId) {
//			if(stops2come.contains(this.stopToGo)){
//				if(this.waitingAt.equals(facilityId)){
//					this.possibleVehiclePassedButNotEntered++;
//				}
//			}
//		}
//		
//		@Override
//		public String toString(){
//			return (agentId.toString() + ";" + this.time + ";" + waitingAt.toString() + ";" 
//								+ this.stopToGo.toString() + ";" + this.possibleVehiclePassedButNotEntered + ";"
//								+ this.routeNotExisting + ";"); 
//		}
//
//	}
//	
}

