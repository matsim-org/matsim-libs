package org.matsim.run.winnerlosertable;

import org.matsim.gbl.Gbl;

public class WinnerLoserTable {
	
	/**
	 * @param args: 
	 * arg 0: path to plans file 0
	 * arg 1: path to plans file 1
	 * arg 2: name of output file
	 * arg 3: path to network file
	 */
	public static void main(String[] args) {
		
		if (args.length < 4) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
		
		
		Gbl.startMeasurement();
		WinnerLoserReaderWriter winnerLoserReaderWriter = new WinnerLoserReaderWriter ();	
		winnerLoserReaderWriter.run(args[0], args[1], args[2], args[3]);
				
		Gbl.printElapsedTime();
	}
	
	private static void printUsage() {
		System.out.println();
		System.out.println("WinnerLoserSummary:");
		System.out.println();
		System.out.println("Creates an agent-based winner-loser table including all agent \n" +
				"attributes, the selected plan score and the total travel time");
		System.out.println();
		System.out.println("usage: WinnerLoserSummary args");
		System.out.println(" arg 0: path to plans file 0 (required)");
		System.out.println(" arg 1: path to plans file 1 (required)");
		System.out.println(" arg 2: name of output file (required)");
		System.out.println(" arg 3: path to network file (required)");

		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}
	
}
