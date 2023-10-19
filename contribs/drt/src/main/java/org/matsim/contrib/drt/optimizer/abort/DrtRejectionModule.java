package org.matsim.contrib.drt.optimizer.abort;

import com.google.inject.Singleton;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;

public class DrtRejectionModule extends AbstractModule {
	@Override
	public void install() {
		ConfigUtils.addOrGetModule(this.getConfig(), QSimComponentsConfigGroup.class).addActiveComponent(BasicDrtAbortHandler.COMPONENT_NAME);

		bind(DrtRejectionEventHandler.class).in(Singleton.class);
		addEventHandlerBinding().to(DrtRejectionEventHandler.class);
		addControlerListenerBinding().to(DrtRejectionEventHandler.class);

		this.installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				this.addQSimComponentBinding(BasicDrtAbortHandler.COMPONENT_NAME).to(BasicDrtAbortHandler.class);
			}
		});
	}
}
