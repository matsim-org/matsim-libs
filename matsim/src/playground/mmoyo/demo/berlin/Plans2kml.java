package playground.mmoyo.demo.berlin;

import playground.andreas.bln.ana.plans2kml.PersonPlan2Kml;

public class Plans2kml {

	public static void main(String[] args) {

		String net ="../shared-svn/studies/schweiz-ivtch/pt-experimental/berlin/network.multimodal.xml"; 
		String outputPlans = "../shared-svn/studies/schweiz-ivtch/pt-experimental/berlin/output/manuelPTsim.output_plans.xml.gz"; 
		String outputDir = "../shared-svn/studies/schweiz-ivtch/pt-experimental/berlin/output/2Kml";
		String agentIds =  "11100153";
		
		PersonPlan2Kml.main(new String[]{net, outputPlans, outputDir , agentIds});
	}
}
