package saleem.p0;



public class P0ControllerWithSignals {
	public static void main(String[] args) {
//		String path = "./ihop2/matsim-input/configSingleJunction.xml";
////		String path = "H:\\Mike Work\\input\\config.xml";
////		Config config = ConfigUtils.loadConfig(path, new SignalSystemsConfigGroup(), new OTFVisConfigGroup() ) ;
////		Config config = ConfigUtils.loadConfig(path, new SignalSystemsConfigGroup()) ;
//		config.network().setTimeVariantNetwork(true);
//	    
//		
//		// Changing vehicle and road capacity according to sample size
////		config.qsim().setInflowConstraint(InflowConstraint.maxflowFromFdiag);
////		config.qsim().setTrafficDynamics(TrafficDynamics.withHoles);
//		
//		final Scenario scenario = ScenarioUtils.loadScenario(config);
//		Controler controler = new Controler(scenario);
//		
//		
//		Network network = scenario.getNetwork();
//		StockholmP0Helper sth = new StockholmP0Helper(network);
//		String nodesfile = "./ihop2/matsim-input/NodesSingleJunction.csv";
//		String pretimedxyxcords = "./ihop2/matsim-input/pretimedxyxcordssinglejunction.xy";
//		List<String> timednodes = sth.getPretimedNodes(nodesfile);
////		List<String> timednodes = sth.getPretimedNodes("H:\\Mike Work\\input\\Nodes2Junctions.csv");
//		Map<String, List<Link>> incominglinks = sth.getInLinksForJunctions(timednodes, network);
//		Map<String, List<Link>> outgoinglinks = sth.getOutLinksForJunctions(timednodes, network);
////		sth.writePretimedNodesCoordinates(nodesfile,pretimedxyxcords);
//		SignalSystemsConfigGroup signalConfig = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class ) ;
//		
//		// the following makes the contrib load  the signalSystems files, but not to do anything with them:
//		// (this switch will eventually go away)
//		signalConfig.setUseSignalSystems(false);
//
//		// these are the paths to the signal systems definition files:
//		signalConfig.setSignalSystemFile("./ihop2/matsim-input/signalSystems_v2.0.xml");
//		signalConfig.setSignalGroupsFile("./ihop2/matsim-input/signalGroups_v2.0.xml");
//		signalConfig.setSignalControlFile("./ihop2/matsim-input/signalControl_v2.0.xml");
//		
////		OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
////		otfvisConfig.setScaleQuadTreeRect(true); // make links visible beyond screen edge
////		otfvisConfig.setColoringScheme(ColoringScheme.byId);
////		otfvisConfig.setAgentSize(120);
//		
//		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(signalConfig).loadSignalsData());
////		controler.getConfig().qsim().setInflowConstraint(InflowConstraint.maxflowFromFdiag);
////		controler.getConfig().qsim().setTrafficDynamics(TrafficDynamics.withHoles);
//		NetworkFactoryImpl nf = network.getFactory();
//		nf.setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
//		
//		controler.addOverridingModule(new SignalsModule());
////		controler.addOverridingModule( new OTFVisWithSignalsLiveModule() );
//
//
////		controler.addControlerListener(new StockholmP0ControlListener(scenario, (NetworkImpl) scenario.getNetwork(), incominglinks, outgoinglinks));
//		controler.run();

}

}
