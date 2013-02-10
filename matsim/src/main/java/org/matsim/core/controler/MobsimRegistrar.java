package org.matsim.core.controler;

import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulationFactory;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;

public class MobsimRegistrar {

	private MobsimFactoryRegister register = new MobsimFactoryRegister();
	
	public MobsimRegistrar() {
		register.register(MobsimType.qsim.toString(), new QSimFactory());
		register.register(MobsimType.queueSimulation.toString(), new QueueSimulationFactory());
		register.register(MobsimType.JDEQSim.toString(), new JDEQSimulationFactory());
	}
	
	public MobsimFactoryRegister getFactoryRegister() {
		return register;
	}
	
}
