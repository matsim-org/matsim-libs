package ConfigCreator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunMatsimUnique {

	public static void main(String[] args) {

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\config19.xml", new MultiModeDrtConfigGroup(),
				new DvrpConfigGroup(), new OTFVisConfigGroup() );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setOutputDirectory("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\output");

		// possibly modify config here
		config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSimEndtimeInterpretation((QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime));

		// ---
		Controler controler = DrtControlerCreator.createControler(config, false);

		// possibly modify scenario here

		// ---

		// possibly modify controler here

//		controler.addOverridingModule( new OTFVisLiveModule() ) ;


		// ---

		controler.run();
	}
}
