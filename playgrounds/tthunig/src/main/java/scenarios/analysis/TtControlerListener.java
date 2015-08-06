package scenarios.analysis;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * Class to bind the analyze tool (given in the constructor) and the writing
 * tool to the simulation. It works for all analyze tools that extend the
 * abstract analyze tool TtAbstractAnalyzeTool.
 * 
 * @author tthunig
 */
public class TtControlerListener implements StartupListener, IterationEndsListener {

	Scenario scenario;
	TtAbstractAnalysisTool handler;
	TtAnalyzedResultsWriter writer;
	
	public TtControlerListener(Scenario scenario, TtAbstractAnalysisTool handler) {
		this.scenario = scenario;
		this.handler = handler;
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
		event.getControler().getEvents().addHandler(handler);
		
		// prepare the results writer
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "analysis/";
		new File(outputDir).mkdir();
		this.writer = new TtAnalyzedResultsWriter(handler, outputDir);
	}

}
