package playground.mmoyo.demo.X5;

import playground.mmoyo.demo.ScenarioDemo;

public class Simple1X5NetDemo {

	public static void main(final String[] args) {
		String networkFile = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5X5/network.xml";
		String plansFile = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5X5/Plans.xml";
		String outputDirectory = "./output/X5demo";
		String transitScheduleFile = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5X5/simple1TransitSchedule.xml";
		String vehiclesFile = "../shared-svn/studies/schweiz-ivtch/pt-experimental/5X5/vehicles.xml";
		ScenarioDemo.main(new String[]{networkFile, plansFile, outputDirectory, transitScheduleFile, vehiclesFile}, true);

	}

}
