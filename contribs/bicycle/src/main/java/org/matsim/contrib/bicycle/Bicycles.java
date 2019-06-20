package org.matsim.contrib.bicycle;

import org.matsim.core.controler.AllowsConfiguration;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

public class Bicycles {

	public static void addAsOverridingModule(AllowsConfiguration ao) {

		ao.addOverridingModule(new BicycleModule());
		ao.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(QNetworkFactory.class).toProvider(BicycleModule.QNetworkFactoryProvider.class);
			}
		})
	}
}
