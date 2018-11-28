package org.matsim.core.controler;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public interface AllowsOverriding{
	AllowsOverriding addOverridingModule( AbstractModule abstractModule );
	AllowsOverriding addOverridingQSimModule( AbstractQSimModule qsimModule );
}
