package playground.mmoyo.analysis.comp;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import playground.mmoyo.PTRouter.PTValues;

public class AllRouter {

	/**routes all scenarios with the 3 cost calculations and increasing parameters coefficients**/
	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		/*
		Do not forget to set:
		-name of scenario
		-get ride of autos?
		-split the plans?
		-cost coefficients
		-only plans inside the investigation area?
		*/

		PTValues.scenario = "BerBran_1x_subset_xy2links_nocarplans";
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/config_noRouted/configWithoutRouting_1x_subset_xy2links_nocarplans.xml";
		
		PTValues.scenario = "BerBran_1x_subset_xy2links_ptplansonly";
		//  configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/config_noRouted/configWithoutRouting_1x_subset_xy2links_ptplansonly.xml";
		
		PTValues.routerCalculator = 1;
		PlanRouter.main(new String[]{configFile});
		
		PTValues.routerCalculator = 2;
		PlanRouter.main(new String[]{configFile});

		PTValues.routerCalculator = 3;
		PTValues.distanceCoefficient =0.15;
		PTValues.timeCoefficient = 0.85;
		PTValues.transferPenalty = 60.0;
		PlanRouter.main(new String[]{configFile});
	}
}
