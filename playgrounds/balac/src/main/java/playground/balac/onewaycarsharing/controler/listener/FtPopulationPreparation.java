package playground.balac.onewaycarsharing.controler.listener;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;

import playground.balac.onewaycarsharing.config.OneWayCSConfigGroup;
import playground.meisterk.kti.population.algorithms.PersonDeleteNonKtiCompatibleRoutes;
import playground.meisterk.kti.population.algorithms.PersonInvalidateScores;



public class FtPopulationPreparation implements StartupListener {

	private final OneWayCSConfigGroup ftConfigGroup;
	
	public FtPopulationPreparation(OneWayCSConfigGroup ftConfigGroup) {
		super();
		this.ftConfigGroup = ftConfigGroup;
	}

	public void notifyStartup(StartupEvent event) {

		Population pop = event.getControler().getPopulation();
		Config config = event.getControler().getConfig();
		
		/*
		 * make sure every pt leg has a kti pt route when the kti pt router is used
		 */
		if (this.ftConfigGroup.isUsePlansCalcRouteFt()) {
			ParallelPersonAlgorithmRunner.run(
					pop, 
					config.global().getNumberOfThreads(),
					new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
						public AbstractPersonAlgorithm getPersonAlgorithm() {
							return new PersonDeleteNonKtiCompatibleRoutes();
						}
					});
		}
		if (this.ftConfigGroup.isInvalidateScores()) {
			ParallelPersonAlgorithmRunner.run(
					pop, 
					config.global().getNumberOfThreads(),
					new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
						public AbstractPersonAlgorithm getPersonAlgorithm() {
							return new PersonInvalidateScores();
						}
					});
		}
	}
}
