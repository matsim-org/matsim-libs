package org.matsim.dsim;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.dsim.simulation.DSimComponentsModule;
import org.matsim.dsim.simulation.SimProvider;
import org.matsim.dsim.simulation.net.NetworkTrafficModule;
import org.matsim.dsim.simulation.pt.DistributedPtModule;

/**
 * Defines modules for {@link DSim}.
 */
public class DSimModule extends AbstractModule {

	@Override
	public void install() {

		bind(DSim.class).in(Singleton.class);
		bindMobsim().toProvider(DSimProvider.class);

		// Install default Qsim which are reused by DSim
		installQSimModule(new ActivityEngineModule());
		installQSimModule(new TransitEngineModule());

		// DSim specific QSim modules
		installQSimModule(new DSimComponentsModule());
		installQSimModule(new NetworkTrafficModule());
		installQSimModule(new DistributedPtModule());

		DistributedSimulationModule.bindSimulationProcess(binder())
			.to(SimProvider.class);
	}

}
