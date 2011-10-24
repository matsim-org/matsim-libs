package playground.wdoering.debugvisualization;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsManagerImplTest;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.wdoering.debugvisualization.controller.*;

/**
 * Debug Visualization
 * 
 * video editing akin exploration of events
 * displaying traces of agents.
 * 
 * @author wdoering
 *
 */
public class DebugVisualization {

	
	public static void main(String[] args) {
		
		//console interface for status and debug tracking
		Console console = new ConsoleImpl(false);
		
		Config c = ConfigUtils.createConfig();
		
		c.network().setInputFile(args[1]);
		
		Scenario sc = ScenarioUtils.loadScenario(c);
		//sc.getNetwork().get -> nur über Links (hat from / to nodes (getcoord (get x y)))
		
		EventsManager e = EventsUtils.createEventsManager();
		
		
		
		//argument syntax: DebugSim.java eventfile.xml networkfile.xml liveMode [=true / false / null||else(=false) ]
		if (args.length > 0)
		{
			//console.println("Initializing Debug Simulation.");
			//Controller controller = new Controller(args[0], args[1], console, 3, true);
			Controller controller = new Controller(e, sc);
			
			//XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(e);
			//reader.parse(args[0]);
			
		}
		else
		{
			console.println("Too few arguments.");
			System.exit(0);
		}
		
		
	}

}
