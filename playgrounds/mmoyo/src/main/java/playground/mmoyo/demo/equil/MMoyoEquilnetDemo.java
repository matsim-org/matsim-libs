package playground.mmoyo.demo.equil;

import playground.mmoyo.demo.ScenarioDemo;

public class MMoyoEquilnetDemo {

	public static void main(final String[] args) {
		boolean launchOTFDemo = true;
		String networkFile = "../matsim/examples/equil/network.xml";
		String plansFile = "../matsim/examples/equil/plans2.xml";
		String outputDirectory = "./output/transitEquil2";
		String transitScheduleFile = "../playgrounds/mrieser/src/main/java/playground/mrieser/pt/demo/equilnet/transitSchedule.xml";
		String vehiclesFile = "../playgrounds/mrieser/src/main/java/playground/mrieser/pt/demo/equilnet/vehicles.xml";
		if (args.length>0 && args[0].equals("NoOTFDemo")) launchOTFDemo=false ;
		ScenarioDemo.main(new String[]{networkFile, plansFile, outputDirectory, transitScheduleFile, vehiclesFile}, launchOTFDemo);
	}
}
