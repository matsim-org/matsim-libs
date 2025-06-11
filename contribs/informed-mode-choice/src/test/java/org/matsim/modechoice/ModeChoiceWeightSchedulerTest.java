package org.matsim.modechoice;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;

import static org.assertj.core.api.Assertions.assertThat;

public class ModeChoiceWeightSchedulerTest extends ScenarioTest {

	@Test
	void linear() {

		controler.getConfig().controller().setLastIteration(100);
		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(controler.getConfig(), InformedModeChoiceConfigGroup.class);

		imc.setInvBeta(1);
		imc.setAnneal(InformedModeChoiceConfigGroup.Schedule.linear);

		ModeChoiceWeightScheduler scheduler = new ModeChoiceWeightScheduler(controler.getConfig());
		MatsimServices services = injector.getInstance(MatsimServices.class);
		scheduler.notifyStartup(new StartupEvent(services));

		assertThat(scheduler.getInvBeta()).isEqualTo(1);

		scheduler.notifyIterationStarts(new IterationStartsEvent(services, 1, false));
		assertThat(scheduler.getInvBeta()).isEqualTo(1);

		scheduler.notifyIterationStarts(new IterationStartsEvent(services, 10, false));
		assertThat(scheduler.getInvBeta()).isCloseTo(0.89, Offset.offset(0.01));

		scheduler.notifyIterationStarts(new IterationStartsEvent(services, 90, false));
		assertThat(scheduler.getInvBeta()).isEqualTo(0);

	}

	@Test
	void quadratic() {

		controler.getConfig().controller().setLastIteration(101);
		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(controler.getConfig(), InformedModeChoiceConfigGroup.class);

		imc.setInvBeta(1);
		imc.setAnneal(InformedModeChoiceConfigGroup.Schedule.quadratic);

		ModeChoiceWeightScheduler scheduler = new ModeChoiceWeightScheduler(controler.getConfig());
		MatsimServices services = injector.getInstance(MatsimServices.class);
		scheduler.notifyStartup(new StartupEvent(services));

		assertThat(scheduler.getInvBeta()).isEqualTo(1);

		scheduler.notifyIterationStarts(new IterationStartsEvent(services, 1, false));
		assertThat(scheduler.getInvBeta()).isEqualTo(1);

		// exactly 1/4 at 50% iterations
		scheduler.notifyIterationStarts(new IterationStartsEvent(services, 46, false));
		assertThat(scheduler.getInvBeta()).isEqualTo(0.25);

	}
}
