package playground.ciarif.flexibletransports.scenario;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.ciarif.flexibletransports.router.PlansCalcRouteFtInfo;

public class FtScenarioLoaderImpl extends ScenarioLoaderImpl {

	private static final Logger log = Logger.getLogger(FtScenarioLoaderImpl.class);

	private PlansCalcRouteFtInfo plansCalcRouteFtInfo;
	private FtConfigGroup ftConfigGroup;
	
	public FtScenarioLoaderImpl(Scenario scenario, PlansCalcRouteFtInfo plansCalcRouteFtInfo, FtConfigGroup ftConfigGroup) {
		super(scenario);
		this.plansCalcRouteFtInfo = plansCalcRouteFtInfo;
		this.ftConfigGroup = ftConfigGroup;
	}

	@Override
	public Scenario loadScenario() {
		String currentDir = new File("tmp").getAbsolutePath();
		currentDir = currentDir.substring(0, currentDir.length() - 3);
		log.info("loading scenario from base directory: " + currentDir);
//		this.loadWorld();
		this.loadNetwork();
		if (this.ftConfigGroup.isUsePlansCalcRouteFt()) {
			this.loadPlansCalcRouteKtiInfo();
		}
		this.loadActivityFacilities();
		this.loadPopulation();
		
		return getScenario();

	}

	private void loadPlansCalcRouteKtiInfo() {
		this.plansCalcRouteFtInfo.prepare(this.getScenario().getNetwork());
	}
}
