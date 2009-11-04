package playground.mmoyo.demo.berlin;

import playground.mmoyo.demo.ScenarioPlayer;

public class Player {

	public static void main(String[] args) {
		
		//invoke controler first
		
		String networkFile = "../shared-svn/studies/ptsimmanuel/input/network.multimodal.xml";
		String scheduleFile =  "../shared-svn/studies/ptsimmanuel/input/transitSchedule.networkOevModellBln.xml";
		String outputPlansFile = "./output/BerlinDemo/output_plans.xml.gz";  //output of controller, input of player 
		String outputDirectory = ".output/BerlinPlayerOutput";
		ScenarioPlayer.main(new String[]{networkFile, scheduleFile, outputPlansFile, outputDirectory});
	}
}
