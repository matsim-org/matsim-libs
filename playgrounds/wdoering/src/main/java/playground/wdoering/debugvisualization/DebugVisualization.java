package playground.wdoering.debugvisualization;

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
		Console console = new ConsoleImpl();
		
		//argument syntax: DebugSim.java eventfile.xml networkfile.xml liveMode [=true / false / null||else(=false) ]
		if (args.length > 0)
		{
			console.println("Initializing Debug Simulation.");
			Controller controller = new Controller(args[0], args[1], console, false);
			
		}
		else
		{
			console.println("Too few arguments.");
			System.exit(0);
		}
		
		
	}

}
