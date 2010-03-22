package playground.gregor.sims.evacuationdelay;

import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.groups.EvacuationConfigGroup.EvacuationScenario;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.evacuation.base.EvacuationStartTimeCalculator;


public class DelayedEvacuationPopulationLoader extends EvacuationPopulationFromShapeFileLoader {
	private final DelayedEvacuationStartTimeCalculator startTimer;

	public DelayedEvacuationPopulationLoader(List<Building> buildings,ScenarioImpl scenario) {
		super(scenario.getPopulation(),buildings, scenario);
		double baseTime = scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.day ? 12 * 3600 : 3 * 3600;
		this.startTimer = new DelayedEvacuationStartTimeCalculator(baseTime,scenario.getConfig().evacuation().getShorelineFile(), scenario.getNetwork());
	}

	@Override
	protected EvacuationStartTimeCalculator getEndCalculatorTime() {
		return this.startTimer; 
	}


}
