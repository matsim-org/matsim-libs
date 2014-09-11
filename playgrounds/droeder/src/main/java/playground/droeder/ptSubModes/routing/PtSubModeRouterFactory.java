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
package playground.droeder.ptSubModes.routing;


/**
 * @author droeder
 *
 */
public class PtSubModeRouterFactory {//extends PTransitRouterFactory implements IterationStartsListener, StartupListener{
//	private static final Logger log = Logger
//			.getLogger(PtSubModeRouterFactory.class);
//	
//	private boolean routeOnSameMode;
//	private Scenario sc;
//	private TransitRouterConfig tC;
//	private Map<String, TransitRouterNetwork> routerNetworks;
//	private boolean updateRouter = true;
//
//	/**
//	 * Factory to create the <code>PtSubModeDependendRouter</code>
//	 * @param sc
//	 * @param routeOnSameMode, for performance-reasons. Create subModeRouters only if necessary
//	 */
//	public PtSubModeRouterFactory(Controler c, boolean routeOnSameMode) {
//		super(((PConfigGroup)c.getConfig().getModule(PConfigGroup.GROUP_NAME)).getPtEnabler());
//		this.sc = c.getScenario();
//		this.routeOnSameMode = routeOnSameMode;
//		this.routerNetworks = new HashMap<String, TransitRouterNetwork>();
//		this.updateRouter = true;
//		this.tC = new TransitRouterConfig(this.sc.getConfig());
//	}
//	
//	@Override
//	public void createTransitRouterConfig(Config config) {
//		// do nothing
//	}
//	
//	@Override
//	public void updateTransitSchedule(TransitSchedule schedule) {
//		//do nothing
//	}
//	
//	@Override
//	public TransitRouter createTransitRouter() {
//		if(this.updateRouter){
//			this.updateRouterNetworks();
//			this.updateRouter = false;
////			this.set = new PtSubModeRouterSet(this.tC, this.routerNetworks, new TransitRouterNetworkTravelTimeAndDisutility(this.tC), this.routeOnSameMode);
//		}
//		return new PtSubModeRouterSet(this.tC, this.routerNetworks, new TransitRouterNetworkTravelTimeAndDisutility(this.tC), this.routeOnSameMode);
//	}
//	
//	@Override
//	public void notifyIterationStarts(IterationStartsEvent event) {
//		this.updateRouter = true;
//	}
//	
//	@Override
//	public void notifyStartup(StartupEvent event) {
//		this.updateRouter = true;
//	}
//	
//	private void updateRouterNetworks(){
//		//create the default
//		this.routerNetworks.put(TransportMode.pt, 
//				TransitRouterNetwork.createFromSchedule(
//						this.sc.getTransitSchedule(), 
//						this.sc.getConfig().transitRouter().getMaxBeelineWalkConnectionDistance()));
//		if(!this.routeOnSameMode) return; //create additional router only if necessary
//		log.info("separating lines by mode from transitSchedule...");
//		// create an empty schedule per mode
//		Map<String, TransitSchedule> temp = new HashMap<String, TransitSchedule>();
//		for(String s: this.sc.getConfig().transit().getTransitModes()){
//			temp.put(s, new TransitScheduleFactoryImpl().createTransitSchedule());
//		}
//		
//		String mode = null;
//		//parse all lines
//		for(TransitLine line : this.sc.getTransitSchedule().getTransitLines().values()){
//			// check mode of routes (in my opinion mode should be pushed up to line!)
//			for(TransitRoute route: line.getRoutes().values()){
//				if(mode == null){
//					mode = route.getTransportMode();
//				}else{
//					// abort if a route line contains a route of different modes. In my opinion this really makes no sense [dr]
//					if(mode != route.getTransportMode()){
//						throw new IllegalArgumentException("one line must not operate on different transport-modes. ABORT...");
//					}
//				}
//			}
//			// check if transitMode is specified in pt-module
//			if(temp.containsKey(mode)){
//				// add routes
//				temp.get(mode).addTransitLine(line);
//				// and TransitStopFacilities
//				for(TransitRoute route: line.getRoutes().values()){
//					for(TransitRouteStop stop: route.getStops()){
//						if(!temp.get(mode).getFacilities().containsKey(stop.getStopFacility().getId())){
//							temp.get(mode).addStopFacility(stop.getStopFacility());
//						}
//					}
//				}
//			}else{
//				//the mode of a line/route should be available to the agents
//				log.warn("mode " + mode + " of transitline " + line.getId() + " not specified in pt-module. ABORT!");
//			}
//			mode = null;
//		}
//		log.info("finished...");
//		log.info("creating mode-dependend TransitRouterNetworks for: " + temp.keySet().toString());
//		//create ModeDependendRouterNetworks
//		for(Entry<String, TransitSchedule> e: temp.entrySet()){
//			this.routerNetworks.put(e.getKey(), 
//					TransitRouterNetwork.createFromSchedule(e.getValue(), 
//							this.sc.getConfig().transitRouter().getMaxBeelineWalkConnectionDistance()));
//		}
//		log.info("finished");
//	}
}

	