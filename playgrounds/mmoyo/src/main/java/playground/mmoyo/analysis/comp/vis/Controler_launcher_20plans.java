package playground.mmoyo.analysis.comp.vis;

import org.matsim.core.gbl.Gbl;
import org.matsim.run.OTFVis;

import playground.mzilske.bvg09.TransitControler;

public class Controler_launcher_20plans {
	
	public static void main(String[] args) {
		//in order to run this the call to the OTFserver must be commented at controler.runMobSim
		//          //sim.startOTFServer("livesim");
		//          //OTFDemo.ptConnect("livesim", this.config);
		
		String config = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/20plans/config_20plans900s_small.xml";
		TransitControler.main(new String[]{config});
		Gbl.reset();
		OTFVis.main(new String []{"-convert", "../playgrounds/mmoyo/output/ITERS/it.0/0.events.txt.gz", "../shared-svn/studies/countries/de/berlin-bvg09/pt/baseplan_900s_smallnetwork/network.multimodal.xml.gz" , "../playgrounds/mmoyo/output/movie.mvi", "300"});
		OTFVis.main(new String []{"../playgrounds/mmoyo/output/movie.mvi"});
		
		
		
		
		
		//This works without problems
		//String config2 = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/nullFall/config_20plansNull.xml";
		//TransitControler.main(new String[]{config});
		}
}
