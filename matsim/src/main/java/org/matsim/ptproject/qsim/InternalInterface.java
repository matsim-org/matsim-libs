package org.matsim.ptproject.qsim;

import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;

public interface InternalInterface {
	public Netsim getMobsim() ; 
	public void arrangeNextAgentState(MobsimAgent agent) ;
}
