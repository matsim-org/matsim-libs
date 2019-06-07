package org.matsim.core.controler;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;

public interface AllowsConfiguration{
	AllowsConfiguration addOverridingModule( AbstractModule abstractModule );
	AllowsConfiguration addOverridingQSimModule( AbstractQSimModule qsimModule );
	AllowsConfiguration addQSimModule( AbstractQSimModule qsimModule ) ;
	AllowsConfiguration configureQSimComponents( QSimComponentsConfigurator configurator ) ;
}
