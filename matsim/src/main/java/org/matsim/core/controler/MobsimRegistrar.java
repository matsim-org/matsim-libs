package org.matsim.core.controler;

import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;

public class MobsimRegistrar {

	private MobsimFactoryRegister register = new MobsimFactoryRegister();
	
	public MobsimRegistrar() {
		register.register(MobsimType.qsim.toString(), ControlerDefaults.createDefaultQSimFactory() );
		register.register(MobsimType.JDEQSim.toString(), ControlerDefaults.createDefaultJDEQSimFactory() ) ;
	}
	
	public MobsimFactoryRegister getFactoryRegister() {
		return register;
	}
	
}
