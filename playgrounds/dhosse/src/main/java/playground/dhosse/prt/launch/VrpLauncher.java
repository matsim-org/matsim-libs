package playground.dhosse.prt.launch;


public class VrpLauncher {
	
//	private PrtParameters params;
//	
//	private Scenario scenario;
//	private TravelTimeSource ttimeSource;
//	private TravelDisutilitySource tdisSource;
//	private TravelTime travelTime;
//	private TravelDisutility travelDisutility;
//	private LeastCostPathCalculator router;
//	private LeastCostPathCalculatorWithCache routerWithCache;
//	private VrpPathCalculator calculator;
//	
//	private MatsimVrpContextImpl context;
//	
//	public VrpLauncher(PrtParameters params){
//		this.params = params;
//		this.scenario = VrpLauncherUtils.initScenario(this.params.pathToNetworkFile, this.params.pathToPlansFile);
//		this.init(this.scenario);
//	}
//	
//	public VrpLauncher(String netFile, String plansFile, String eventsFileName){
//		
//		this.scenario = VrpLauncherUtils.initScenario(netFile, this.params.pathToPlansFile);
//		this.init(this.scenario);
//		
//	}
//	
//	public void init(Scenario scenario){
//		
//		this.ttimeSource = this.params.algorithmConfig.ttimeSource;
//        this.tdisSource = this.params.algorithmConfig.tdisSource;
//
//        this.travelTime = VrpLauncherUtils.initTravelTime(scenario, this.params.algorithmConfig.ttimeSource, this.params.pathToEventsFile);
//        this.travelDisutility = VrpLauncherUtils.initTravelDisutility(this.params.algorithmConfig.tdisSource, this.travelTime);
//
//        this.router = new Dijkstra(scenario.getNetwork(), this.travelDisutility, this.travelTime);
//
//        this.routerWithCache = new LeastCostPathCalculatorWithCache(
//                this.router, new TimeDiscretizer(31*4, 15 *60,false));
//        
//        this.calculator = new VrpPathCalculatorImpl(this.routerWithCache, this.travelTime,
//                this.travelDisutility);
//		
//	}
//	
//	public void run(){
//		
//		this.context = new MatsimVrpContextImpl();
//        this.context.setScenario(this.scenario);
//        Config config = scenario.getConfig();
//        config.qsim().setFlowCapFactor(1.);
//        config.qsim().setStorageCapFactor(1.);
//        config.qsim().setEndTime(30*3600);
//        config.controler().setOutputDirectory("C:/Users/Daniel/Desktop/dvrp/");
//        config.controler().setCreateGraphs(true);
//        
//        TaxiData taxiData = TaxiLauncherUtils.initTaxiData(scenario, params.pathToTaxisFile,
//        		params.pathToRanksFile);
//        
//        context.setVrpData(taxiData);
//        PrtData prtData = new PrtData(scenario.getNetwork(), taxiData);
//        
//        PrtTripRouterFactoryImpl tripRouterFactory = new PrtTripRouterFactoryImpl(this.context, this.travelTime, this.travelDisutility);
//        final PersonAlgorithm router = new PlanRouter(tripRouterFactory.get(new RoutingContextImpl(this.travelDisutility, this.travelTime)));
//
//        for(Person p : scenario.getPopulation().getPersons().values()){
//        	
//        	if(p.getId().toString().contains("prt"))
//        		router.run(p);
//        }
//        
//        TaxiOptimizerConfiguration optimConfig = initOptimizerConfiguration();
//        TaxiOptimizer optimizer = params.algorithmConfig.createTaxiOptimizer(optimConfig);
////        APSTaxiOptimizer optimizer = new APSTaxiOptimizer(optimConfig);
////        RESTaxiOptimizer optimizer = new RESTaxiOptimizer(optimConfig);
////        PrtNPersonsOptimizer optimizer = new PrtNPersonsOptimizer(optimConfig);
////        OTSTaxiOptimizer optimizer = new OTSTaxiOptimizer(optimizerConfig);
////        NOSTaxiOptimizer optimizer = new NOSTaxiOptimizer(optimConfig);
//        
//        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
//        context.setMobsimTimer(qSim.getSimTimer());
//        
//        qSim.addQueueSimulationListeners(optimizer);
//
//        //passenger engine
//        PassengerEngine passengerEngine = new PassengerEngine(PrtRequestCreator.MODE, new PrtRequestCreator(), optimizer,
//                context);
//        qSim.addMobsimEngine(passengerEngine);
//        qSim.addDepartureHandler(passengerEngine);
//        
//        if(this.params.nPersons){
//        	VrpLauncherUtils.initAgentSources(qSim, context, optimizer, new NPersonsActionCreator(
//        			passengerEngine, VrpLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR, params.pickupDuration));
//        } else{
//        	VrpLauncherUtils.initAgentSources(qSim, context, optimizer, new TaxiActionCreator(
//        			passengerEngine, VrpLegs.LEG_WITH_OFFLINE_TRACKER_CREATOR, params.pickupDuration));
//        }
//
//        EventsManager events = qSim.getEventsManager();
//        
//        EventWriterXML writer = new EventWriterXML("C:/Users/Daniel/Desktop/dvrp/events.xml");
//        events.addHandler(writer);
//        
//        PopulationWriter pWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
//        pWriter.write("C:/Users/Daniel/Desktop/dvrp/" + "person.xml");
//        
//        if (this.params.otfVis) { // OFTVis visualization
//            DynAgentLauncherUtils.runOTFVis(qSim, false, ColoringScheme.taxicab);
//        }
//        
//        LegHistogram legHistogram = new LegHistogram(300, 360);// LegHistogram(300);
//        events.addHandler(legHistogram);
//
//        qSim.run();
//        
//        events.finishProcessing();
//        writer.closeFile();
//        legHistogram.write("C:/Users/Daniel/Desktop/dvrp/hist.txt");
//        LegHistogramChart.writeGraphic(legHistogram, "C:/Users/Daniel/Desktop/dvrp/hist_all.png");
//        LegHistogramChart.writeGraphic(legHistogram, "C:/Users/Daniel/Desktop/dvrp/hist_prt.png", "prt");
//        LegHistogramChart.writeGraphic(legHistogram, "C:/Users/Daniel/Desktop/dvrp/hist_car.png", TransportMode.car);
//        LegHistogramChart.writeGraphic(legHistogram, "C:/Users/Daniel/Desktop/dvrp/hist_transitWalk.png", TransportMode.transit_walk);
//        
//        generateOutput();
//        
//	}
//	
//	void generateOutput()
//    {
//        PrintWriter pw = new PrintWriter(System.out);
////        pw.println(params.algorithmConfig.name());
//        pw.println("m\t" + context.getVrpData().getVehicles().size());
//        pw.println("n\t" + context.getVrpData().getRequests().size());
//        pw.println(TaxiStats.HEADER);
//        TaxiStats stats = new TaxiStatsCalculator().calculateStats(context.getVrpData()
//                .getVehicles());
//        pw.println(stats);
//        pw.flush();
//
//        // ChartUtils.showFrame(RouteChartUtils.chartRoutesByStatus(data.getVrpData()));
//        ChartUtils.showFrame(TaxiScheduleChartUtils.chartSchedule(context.getVrpData()
//                .getVehicles()));
//
////        if (params.histogramOutDir != null) {
////            VrpLauncherUtils.writeHistograms(legHistogram, params.histogramOutDir);
////        }
//    }
//	
//	private TaxiOptimizerConfiguration initOptimizerConfiguration(){
//		
//		if(this.params.nPersons){
//			
//			PrtScheduler scheduler = new PrtScheduler(this.context, this.calculator, this.params.taxiParams);
//			NPersonsVehicleRequestPathFinder vrpFinder = new NPersonsVehicleRequestPathFinder(this.calculator, scheduler, this.params.capacity);
//			FilterFactory filterFactory = new DefaultFilterFactory(scheduler, 0, 0);
//			
//			return new TaxiOptimizerConfiguration(this.context, this.calculator, scheduler, vrpFinder, filterFactory,
//					this.params.algorithmConfig.goal, this.params.workingDir);
//			
//		}
//		
//		TaxiScheduler scheduler = new TaxiScheduler(this.context, this.calculator, this.params.taxiParams);
//		VehicleRequestPathFinder vrpFinder = new VehicleRequestPathFinder(this.calculator, scheduler);
//		FilterFactory filterFactory = new DefaultFilterFactory(scheduler, 0, 0);
//		
//		return new TaxiOptimizerConfiguration(this.context, this.calculator, scheduler, vrpFinder, filterFactory,
//				this.params.algorithmConfig.goal, this.params.workingDir);
//		
//	}
//	
//	public Scenario getScenario() {
//		return scenario;
//	}
//
//	public TravelTimeSource getTtimeSource() {
//		return ttimeSource;
//	}
//
//	public TravelDisutilitySource getTdisSource() {
//		return tdisSource;
//	}
//
//	public TravelTime getTravelTime() {
//		return travelTime;
//	}
//
//	public TravelDisutility getTravelDisutility() {
//		return travelDisutility;
//	}
//
//	public LeastCostPathCalculator getRouter() {
//		return router;
//	}
//
//	public LeastCostPathCalculatorWithCache getRouterWithCache() {
//		return routerWithCache;
//	}
//
//	public VrpPathCalculator getCalculator() {
//		return calculator;
//	}

}
