/**
 * 
 */
package playground.jbischoff.BAsignals.controler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.controler.SignalsControllerListener;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.SignalGroupState;
import org.matsim.signalsystems.model.SignalSystemsManager;

import playground.dgrether.signalsystems.analysis.DgSignalGreenSplitHandler;
import playground.dgrether.signalsystems.analysis.DgSignalGroupAnalysisData;
import playground.jbischoff.BAsignals.CottbusMain;
import playground.jbischoff.BAsignals.analysis.TimeCalcHandler;
import playground.jbischoff.BAsignals.builder.JbSignalBuilder;
import playground.jbischoff.BAsignals.model.AdaptiveControllHead;
import playground.jbischoff.BAsignals.model.CarsOnLaneHandler;

/**
 * @author jbischoff
 * 
 */
public class JBSignalControllerListener implements StartupListener, IterationStartsListener, ShutdownListener,
		SignalsControllerListener {

	private JbSignalBuilder jbBuilder;
	private SignalSystemsManager manager;
	private CarsOnLaneHandler collh;
	private TimeCalcHandler tch;
	private AdaptiveControllHead ach;

	
	private DgSignalGreenSplitHandler signalGreenSplitHandler;

	public JBSignalControllerListener() {
		this.collh = new CarsOnLaneHandler();
		this.ach = new AdaptiveControllHead();
		this.tch = new TimeCalcHandler(this.ach);
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
		SignalSystemsConfigGroup signalsConfig = scenario.getConfig().signalSystems();
		SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
		// this.loadData(signalsConfig, scenario);
		FromDataBuilder builder = new FromDataBuilder(signalsData, c.getEvents());
		jbBuilder = new JbSignalBuilder(signalsData, builder, this.collh, this.ach);
		this.manager = jbBuilder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		c.getQueueSimulationListener().add(engine);
		c.getEvents().addHandler(tch);
	}	
	
	@Override
	public void notifyShutdown(ShutdownEvent e){
		this.writeData(e.getControler().getScenario(), e.getControler().getControlerIO());
	}

	private void addControlerListeners(Controler c) {
		// strange compilation error
		signalGreenSplitHandler = new DgSignalGreenSplitHandler();
		signalGreenSplitHandler.addSignalSystem(new IdImpl("18"));
		signalGreenSplitHandler.addSignalSystem(new IdImpl("17"));
		signalGreenSplitHandler.addSignalSystem(new IdImpl("1"));
		signalGreenSplitHandler.addSignalSystem(new IdImpl("28"));
		signalGreenSplitHandler.addSignalSystem(new IdImpl("27"));
		signalGreenSplitHandler.addSignalSystem(new IdImpl("12"));


		c.getEvents().addHandler(signalGreenSplitHandler);
		c.addControlerListener(new StartupListener() {

			public void notifyStartup(StartupEvent e) {
				e.getControler().getEvents()
						.addHandler(signalGreenSplitHandler);
			}
		});

		
		c.addControlerListener(new IterationEndsListener() {
			private final Logger logg = Logger
			.getLogger(IterationEndsListener.class);
			public void notifyIterationEnds(IterationEndsEvent e) {
				logg.info("Agents that passed an adaptive signal system (1,17 or 18) at least once: "
						+ tch.getPassedAgents());

		
		logg.info("Average TravelTime for Agents that passed an adaptive signal at least once: "+tch.getAverageAdaptiveTravelTime());
		logg.info("Average TT of all Agents" +tch.getAverageTravelTime() ); 
		tch.exportArrivalTime(e.getIteration(), e.getControler().getConfig().controler().getOutputDirectory());
		logg.info("Latest arrival time at stadium for Agents coming from Cottbus: "+tch.getLatestArrivalCBSDF());
		logg.info("Latest arrival time at stadium for Agents coming from SPN: "+tch.getLatestArrivalSPNSDF());
		logg.info("Latest home time for agents going from stadium to Cottbus"+tch.getLatestArrivalSDFCB());
		logg.info("Latest home time for agents going from stadium to SPN"+tch.getLatestArrivalSDFSPN());

		
			}
		});

		c.addControlerListener(new ShutdownListener() {
			private final Logger logg = Logger
					.getLogger(ShutdownListener.class);

			public void notifyShutdown(ShutdownEvent e) {
				try {
					FileWriter fw = new FileWriter(e.getControler().getConfig().controler().getOutputDirectory()+"signal_statistic.csv");
					for (Id ssid : signalGreenSplitHandler.getSystemIdAnalysisDataMap().keySet()) {
						for (Entry<Id, DgSignalGroupAnalysisData> entry : signalGreenSplitHandler
								.getSystemIdAnalysisDataMap().get(ssid)
								.getSystemGroupAnalysisDataMap().entrySet()) {
							// logg.info("for signalgroup: "+entry.getKey());
							for (Entry<SignalGroupState, Double> ee : entry
									.getValue().getStateTimeMap().entrySet()) {
								// logg.info(ee.getKey()+": "+ee.getValue());
							fw.append(ssid + ";" + entry.getKey() + ";"
										+ ee.getKey() + ";" + ee.getValue()+";\n");

							}
						}
					}				
				fw.flush();
				fw.close();
				logg.info("Wrote signalsystemstats.");
				
				
				
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}


			}
		}
				);

	
			}

	public void writeData(Scenario sc, ControlerIO controlerIO) {
		SignalsData data = sc.getScenarioElement(SignalsData.class);
		new SignalsScenarioWriter(controlerIO).writeSignalsData(data);
	}

}
