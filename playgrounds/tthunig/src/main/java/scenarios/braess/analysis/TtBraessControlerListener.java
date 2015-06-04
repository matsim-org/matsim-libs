package scenarios.braess.analysis;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author tthunig
 */
public class TtBraessControlerListener implements StartupListener, IterationEndsListener {

	Scenario scenario;
	TtBraessResultsWriter writer;
	
	public TtBraessControlerListener(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// write analyzed data
		writer.addSingleItToResults(event.getIteration());
		
		// write final analysis for the last iteration
		if (event.getIteration() == scenario.getConfig().controler().getLastIteration()){
			writer.writeFinalResults();
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// add the analysis tool as events handler to the events manager
		TtAnalyzeBraessRouteDistributionAndTT handler = new TtAnalyzeBraessRouteDistributionAndTT();
		event.getControler().getEvents().addHandler(handler);
		
		// prepare the results writer
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "analysis/";
		new File(outputDir).mkdir();
		this.writer = new TtBraessResultsWriter(handler, outputDir);
	}

}
