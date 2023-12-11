package org.matsim.contrib.simulatedannealing;

import com.google.inject.TypeLiteral;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.contrib.simulatedannealing.acceptor.DefaultAnnealingAcceptor;
import org.matsim.contrib.simulatedannealing.cost.CostCalculator;
import org.matsim.contrib.simulatedannealing.perturbation.ChainedPerturbatorFactory;
import org.matsim.contrib.simulatedannealing.perturbation.PerturbatorFactory;
import org.matsim.testcases.MatsimTestUtils;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author nkuehnel / MOIA
 */
public class SimulatedAnnealingIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testIntegratedAnnealingInQSim() {

		final Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));

		final Controler controler = new Controler(config);

		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setWriteEventsInterval(0);

		SimulatedAnnealingConfigGroup simAnCfg = new SimulatedAnnealingConfigGroup();
		config.controller().setLastIteration(10);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				bind(new TypeLiteral<SimulatedAnnealing<VolumeEstimator>>(){}).toProvider(new Provider<>() {

					@Inject
					CostCalculator<VolumeEstimator> costCalculator;

					@Inject
					PerturbatorFactory<VolumeEstimator> perturbatorFactory;

					@Override
					public SimulatedAnnealing<VolumeEstimator> get() {
						return new SimulatedAnnealing<>(costCalculator, new DefaultAnnealingAcceptor<>(simAnCfg),
								perturbatorFactory, new VolumeEstimator(0), simAnCfg.coolingSchedule, simAnCfg);
					}
				}).asEagerSingleton();

				bind(VolumeEstimator.class).toProvider(new TypeLiteral<SimulatedAnnealing<VolumeEstimator>>(){});

				VolumeCostCalculator costCalculator = new VolumeCostCalculator();
				bind(new TypeLiteral<CostCalculator<VolumeEstimator>>(){}).toInstance(costCalculator);
				addEventHandlerBinding().toInstance(costCalculator);

				bind(new TypeLiteral<PerturbatorFactory<VolumeEstimator>>(){}).toInstance(
						new ChainedPerturbatorFactory.Builder<VolumeEstimator>()
								.add((iteration, temperature) -> current -> {
									return new VolumeEstimator((int) (current.estimation * MatsimRandom.getRandom().nextDouble(2.)));
								}, 1)
								.add((iteration, temperature) -> current -> {
									return new VolumeEstimator(current.estimation + MatsimRandom.getRandom().nextInt(10) - 5);
								}, 1)
								.initialTemperature(simAnCfg.initialTemperature)
								.maxPerturbations(3)
								.minPerturbations(1)
								.build()
				);

				addControlerListenerBinding().to(new TypeLiteral<SimulatedAnnealing<VolumeEstimator>>() {}).asEagerSingleton();

				addControlerListenerBinding().toProvider(new Provider<>() {

					@Inject
					MatsimServices matsimServices;

					@Inject
					SimulatedAnnealing<VolumeEstimator> simulatedAnnealing;

					@Override
					public ControlerListener get() {
						return new SimulatedAnnealingAnalysis<>(getConfig(), matsimServices, simulatedAnnealing);
					}
				});
			}
		});

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addMobsimScopeEventHandlerBinding().toInstance(new MobsimScopeEventHandler() {

					@Inject
					VolumeEstimator volumeEstimator;

					public void cleanupAfterMobsim(int iteration) {
						System.out.println(volumeEstimator.estimation);
					}
				});
			}
		});



		controler.run();
	}

	private static class VolumeCostCalculator implements CostCalculator<VolumeEstimator>, LinkLeaveEventHandler {

		private int counter = 0;

		@Override
		public double calculateCost(VolumeEstimator solution) {
			return Math.abs(counter - solution.estimation);
		}

		@Override
		public void reset(int iteration) {
			counter = 0;
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if(Id.createLinkId(15).equals(event.getLinkId())) {
				counter++;
			}
		}
	}

	static class VolumeEstimator {

		private final int estimation;

		VolumeEstimator(int estimation) {
			this.estimation = estimation;
		}
	}
}
