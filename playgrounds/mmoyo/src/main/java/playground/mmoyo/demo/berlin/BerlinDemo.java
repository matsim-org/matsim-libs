package playground.mmoyo.demo.berlin;

import playground.mmoyo.demo.ScenarioDemo;

public class BerlinDemo {

	/**runs a pt-simulation of 20 agent in Berlin scenario*/
	public static void main(final String[] args) {
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_900s_smallnetwork/network.multimodal.xml.gz";
		String plansFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/20plans/20plans.xml";
		String outputDirectory = "../playgrounds/mmoyo/output";
		String transitScheduleFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_900s_smallnetwork/transitSchedule.networkOevModellBln.xml.gz";
		String vehiclesFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_900s_smallnetwork/vehicles.oevModellBln.xml.gz";
		ScenarioDemo.main(new String[]{networkFile, plansFile, outputDirectory, transitScheduleFile, vehiclesFile}, true);
	}
}
