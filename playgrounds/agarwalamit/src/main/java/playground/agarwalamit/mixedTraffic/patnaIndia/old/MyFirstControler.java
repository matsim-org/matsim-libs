package playground.agarwalamit.mixedTraffic.patnaIndia.old;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.PatnaUtils;
import playground.ikaddoura.analysis.welfare.WelfareAnalysisControlerListener;

public class MyFirstControler {

	static final String  outputDir ="../../../repos/runs-svn/patnaIndia/run105/";
	static final boolean seepage = true;
	
	public static void main(String[] args) {

		WriteConfig cnfg = new WriteConfig();
		cnfg.configRun();
		
		Config config = cnfg.getPatnaConfig();
		Scenario sc = ScenarioUtils.loadScenario(config);

		sc.getConfig().qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);
		
		sc.getConfig().qsim().setLinkDynamics(LinkDynamics.PassingQ.toString());

		PatnaUtils.createAndAddVehiclesToScenario(sc);

		sc.getConfig().qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
		
		final Controler controler = new Controler(sc);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.setDumpDataAtEnd(true);
        controler.getConfig().controler().setCreateGraphs(true);
		controler.addOverridingModule(new OTFVisFileWriterModule());

		controler.addControlerListener(new WelfareAnalysisControlerListener((MutableScenario)controler.getScenario()));
		controler.setDumpDataAtEnd(true);
		controler.run();
	}
}
