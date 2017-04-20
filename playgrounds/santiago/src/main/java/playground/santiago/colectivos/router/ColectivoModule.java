package playground.santiago.colectivos.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.name.Names;

import playground.santiago.SantiagoScenarioConstants;

public class ColectivoModule extends AbstractModule {

	
	@Override
	public void install() {
		if (getConfig().transit().isUseTransit()){
			Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			String scheduleFile = getConfig().transit().getTransitScheduleFileURL(getConfig().getContext()).getFile();
			scheduleFile = scheduleFile.replace("_all", "_colectivo");
			scheduleFile = scheduleFile.replace("%20"," ");
			Logger.getLogger(getClass()).info("Colectivo schedule "+scheduleFile);
			new TransitScheduleReader(scenario2).readFile(scheduleFile);
			bind(TransitSchedule.class).annotatedWith(Names.named(SantiagoScenarioConstants.COLECTIVOMODE)).toInstance(scenario2.getTransitSchedule());
			bind(TransitRouter.class).annotatedWith(Names.named(SantiagoScenarioConstants.COLECTIVOMODE)).toProvider(ColectivoTransitRouterImplFactory.class);
			addRoutingModuleBinding(SantiagoScenarioConstants.COLECTIVOMODE).toProvider(ColectivoRoutingModule.class);
		}
		else {
			throw new RuntimeException("Colectivo system requires public transit.");
		}

	}

}
