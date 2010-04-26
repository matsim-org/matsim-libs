package playground.gregor.sims.evacuationdelay;

import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.EvacuationConfigGroup.EvacuationScenario;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.evacuation.base.EvacuationStartTimeCalculator;
import org.matsim.evacuation.flooding.FloodingReader;


public class DelayedEvacuationPopulationLoader extends EvacuationPopulationFromShapeFileLoader {
	private final DelayedEvacuationStartTimeCalculator startTimer;

	public DelayedEvacuationPopulationLoader(Population populationImpl, List<Building> buildings,ScenarioImpl scenario, List<FloodingReader> netcdfReaders) {
		super(populationImpl,buildings,scenario,netcdfReaders);


		double baseTime = 0;
		 if (scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.day) {
			 baseTime = 12 * 3600;
		 } else if ( scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.night) {
			 baseTime = 3 * 3600;
		 } else if ( scenario.getConfig().evacuation().getEvacuationScanrio() == EvacuationScenario.afternoon) {
			 baseTime = 16 * 3600;
		 }
		this.startTimer = new DelayedEvacuationStartTimeCalculator(baseTime,scenario.getConfig().evacuation().getEvacDecisionZonesFile(), scenario.getNetwork());
	}

	@Override
	protected EvacuationStartTimeCalculator getEndCalculatorTime() {
		return this.startTimer;
	}


}
