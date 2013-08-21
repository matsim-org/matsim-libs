package playground.mzilske.controller;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;

public class MzController {
	
	private Controler controler;

	public final void addControlerListener(ControlerListener l) {
		controler.addControlerListener(l);
	}

	public final void addMobsimFactory(String mobsimName,
			MobsimFactory mobsimFactory) {
		controler.addMobsimFactory(mobsimName, mobsimFactory);
	}

	public final void addSnapshotWriterFactory(String snapshotWriterName,
			SnapshotWriterFactory snapshotWriterFactory) {
		controler.addSnapshotWriterFactory(snapshotWriterName,
				snapshotWriterFactory);
	}

	public final void addPlanStrategyFactory(String planStrategyFactoryName,
			PlanStrategyFactory planStrategyFactory) {
		controler.addPlanStrategyFactory(planStrategyFactoryName,
				planStrategyFactory);
	}

	public final void run() {
		controler.run();
	}
	
	

}
