package playground.mmoyo.demo.berlin.linesM34_344;

import playground.mmoyo.demo.ScenarioDemo;

public class BerlinDemo {

	public static void main(final String[] args) {
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_10x_900s_bignetwork/network.multimodal.xml.gz";
		String plansFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/344plans.xml";
		String outputDirectory = "output/line344";
		String transitScheduleFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/transitScheduleM44_344.xml";
		String vehiclesFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_10x_900s_bignetwork/vehicles.oevModellBln.xml.gz";
		ScenarioDemo.main(new String[]{networkFile, plansFile, outputDirectory, transitScheduleFile, vehiclesFile}, true);
	}
}
