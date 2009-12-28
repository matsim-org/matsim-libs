package playground.mmoyo.demo.berlin;

import playground.mmoyo.demo.ScenarioDemo;

public class BerlinDemo {

	/**runs a pt-simulation of 20 agent in Berlin scenartio*/
	public static void main(final String[] args) {
		String networkFile = "../shared-svn/studies/ptsimmanuel/input/network.multimodal.xml";
		String plansFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/old/pt_only.routedOevModell20.xml";
		String outputDirectory = "./output/BerlinDemo";
		String transitScheduleFile = "../shared-svn/studies/ptsimmanuel/input/transitSchedule.networkOevModellBln.xml";
		String vehiclesFile = "../shared-svn/studies/ptsimmanuel/input/vehicles.oevModellBln.xml";
		ScenarioDemo.main(new String[]{networkFile, plansFile, outputDirectory, transitScheduleFile, vehiclesFile}, true);
	}
}
