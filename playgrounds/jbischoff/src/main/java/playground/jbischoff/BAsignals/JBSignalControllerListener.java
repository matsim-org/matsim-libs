/**
 * 
 */
package playground.jbischoff.BAsignals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.initialization.SignalsControllerListener;
import org.matsim.signalsystems.model.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;

/**
 * @author jbischoff
 *
 */
public class JBSignalControllerListener implements StartupListener, IterationStartsListener, SignalsControllerListener {
	
	private SignalSystemsManager manager;
	private SignalsControllerListener delegate;
	
	public JBSignalControllerListener(SignalsControllerListener delegate){
		this.delegate = delegate;
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.manager.resetModel(event.getIteration());
	}

	@Override
	public void notifyStartup(StartupEvent e) {
		Controler c = e.getControler();
		Scenario scenario = c.getScenario();
		SignalSystemsConfigGroup signalsConfig = scenario.getConfig().signalSystems();
		SignalsData signalsData = this.loadData(signalsConfig, scenario);
		FromDataBuilder builder = new FromDataBuilder(signalsData, c.getEvents());
		JbSignalBuilder jbBuilder = new JbSignalBuilder(signalsData, builder);
		this.manager = jbBuilder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		c.getQueueSimulationListener().add(engine);
	}

	@Override
	public SignalsData loadData(SignalSystemsConfigGroup config, Scenario scenario) {
		return this.delegate.loadData(config, scenario);
	}

	@Override
	public void writeData(Scenario sc, String outputPath) {
		this.delegate.writeData(sc, outputPath);
	}

}
