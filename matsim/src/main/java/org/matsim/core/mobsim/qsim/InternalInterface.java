package org.matsim.core.mobsim.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

public interface InternalInterface {
	public Netsim getMobsim() ; 
	public void arrangeNextAgentState(MobsimAgent agent) ;
	void registerAdditionalAgentOnLink(MobsimAgent agent);
	MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId);
	public void rescheduleActivityEnd(MobsimAgent agent);
}
