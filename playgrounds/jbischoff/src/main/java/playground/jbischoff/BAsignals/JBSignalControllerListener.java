/**
 * 
 */
package playground.jbischoff.BAsignals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.initialization.SignalsControllerListener;
import org.matsim.signalsystems.model.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;
/*
import playground.dgrether.analysis.charts.utils.DgChartWriter;
import playground.dgrether.signalsystems.analysis.DgGreenSplitPerIterationGraph;
import playground.dgrether.signalsystems.analysis.DgSignalGreenSplitHandler;
/*

/**
 * @author jbischoff
 *
 */
public class JBSignalControllerListener implements StartupListener, IterationStartsListener, SignalsControllerListener  {
	private JbSignalBuilder jbBuilder;
	private SignalSystemsManager manager;
	private SignalsControllerListener delegate;
	private CarsOnLaneHandler collh;
//	private DgGreenSplitPerIterationGraph greenSplitPerIterationGraph;
//	private DgGreenSplitPerIterationGraph greenSplitPerIterationGraph1;
//	private DgGreenSplitPerIterationGraph greenSplitPerIterationGraph2;
//
//	private DgSignalGreenSplitHandler signalGreenSplitHandler;
	
	public JBSignalControllerListener(SignalsControllerListener delegate){
		this.collh = new CarsOnLaneHandler();
		this.delegate = delegate;
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.manager.resetModel(event.getIteration());
	}

	@Override
	public void notifyStartup(StartupEvent e) {
		Controler c = e.getControler();
		this.addControlerListeners(c);
			
			
		Scenario scenario = c.getScenario();
		SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
		FromDataBuilder builder = new FromDataBuilder(signalsData, c.getEvents());
		jbBuilder = new JbSignalBuilder(signalsData, builder, this.collh);
		this.manager = jbBuilder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		c.getQueueSimulationListener().add(engine);
		
	}

	private void addControlerListeners(Controler c) {
		// TODO Auto-generated method stub
//		signalGreenSplitHandler = new DgSignalGreenSplitHandler();
//		signalGreenSplitHandler.addSignalSystem(new IdImpl("18"));
//		signalGreenSplitHandler.addSignalSystem(new IdImpl("17"));
//		signalGreenSplitHandler.addSignalSystem(new IdImpl("1"));
//
//		greenSplitPerIterationGraph = new DgGreenSplitPerIterationGraph(c.getConfig().controler(), new IdImpl("18"));
//		greenSplitPerIterationGraph1 = new DgGreenSplitPerIterationGraph(c.getConfig().controler(), new IdImpl("18"));
//		greenSplitPerIterationGraph2 = new DgGreenSplitPerIterationGraph(c.getConfig().controler(), new IdImpl("18"));
//		
//		c.getEvents().addHandler(signalGreenSplitHandler);
//		c.addControlerListener(new StartupListener() {
//
//			public void notifyStartup(StartupEvent e) {
//				e.getControler().getEvents().addHandler(signalGreenSplitHandler);
//			}
//		} );
		
		
		c.addControlerListener((new ShutdownListener() {
			
			public void notifyShutdown(ShutdownEvent e) {
				
				System.err.println("Agents that passed an adaptive signal system at least once: "+collh.getPassedAgents());
				
				
			}}));
		
		c.addControlerListener(new IterationEndsListener() {
			public void notifyIterationEnds(IterationEndsEvent e) {

//				greenSplitPerIterationGraph.addIterationData(signalGreenSplitHandler, e.getIteration());
//				greenSplitPerIterationGraph1.addIterationData(signalGreenSplitHandler, e.getIteration());
//				greenSplitPerIterationGraph2.addIterationData(signalGreenSplitHandler, e.getIteration());
//				}});
		
//		c.addControlerListener(new ShutdownListener() {
//			  private final Logger logg = Logger
//		      .getLogger(ShutdownListener.class);
//			public void notifyShutdown(ShutdownEvent e) {
				

//				DgChartWriter.writeChart(e.getControler().getControlerIO().getOutputFilename("greensplit"), greenSplitPerIterationGraph.createChart());
//				for (Id ssid : signalGreenSplitHandler.getSystemIdAnalysisDataMap().keySet()){
//					//logg.info("=======Statistic for SignalSystem: "+ssid+" =============");
//					/*	for (Entry<Id, DgSignalGroupAnalysisData> entry : signalGreenSplitHandler.getSystemIdAnalysisDataMap().get(ssid).getSystemGroupAnalysisDataMap().entrySet() ){
//logg.info("for signalgroup: "+entry.getKey());
//for (Entry <SignalGroupState,Double> ee : entry.getValue().getStateTimeMap().entrySet()){
//logg.info(ee.getKey()+": "+ee.getValue());
//logg.info("b;"+ssid+";"+entry.getKey()+";"+ee.getKey()+";"+ee.getValue());
//TravelTimeCalculator ttc = e.getControler().getTravelTimeCalculatorFactory().createTravelTimeCalculator(e.getControler().getNetwork(),e.getControler().getConfig().travelTimeCalculator());
//						
//					}
//				}
//				}
//			}
		}}
			)
			;
	}

	public void writeData(Scenario sc, String outputPath) {
		SignalsData data = sc.getScenarioElement(SignalsData.class);
		new SignalsScenarioWriter(outputPath).writeSignalsData(data);
	}




	
}
