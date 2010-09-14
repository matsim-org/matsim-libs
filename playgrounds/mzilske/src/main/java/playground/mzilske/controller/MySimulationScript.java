//package playground.mzilske.controller;
//
//public class MySimulationScript {
//	
//	public static void main(String[] args) {
//		
//		Config config = new Config();
//		fillConfig(config);
//		Scenario scenario = new Scenario();
//		
//		generateInitialDemand(scenario);
//		
//		ReplanningStrategyBuilder b = new ReplanningStrategyBuilder();
//		b.addReplanningModule(new ChangeLegMode())
//		.fromIteration(1)
//		.toIteration(10)
//		.withProbability(0.3);
//		b.addReplanningModule(new ReRoute(scenario.getNetwork()))
//		.fromIteration(5)
//		.toIteration(10)
//		.withProbability(0.5);
//		ReplanningModule replanning = b.build();
//		
//		Scorint s = new DefaultScoring();
//		events.addHandler(s);
//		
//		MyAnalysis a = new MyAnalysis();
//		events.addHandler(a);
//		
//		for (int i = 0; i < 1000; i++) {
//			Mobsim mobsim = new AdvancedParallelMultimodalQueueSim(scenario, events);
//			mobsim.run();
//			s.score(scenario.getPopulation());
//			b.replan(scenario.getPopulation());
//			a.analyze(scenario);
//		}
//		
//		outputScenario(scenario);
//		
//	}
//
//}
