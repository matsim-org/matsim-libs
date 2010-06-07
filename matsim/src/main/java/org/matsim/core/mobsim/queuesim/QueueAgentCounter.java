/**
 * 
 */
package org.matsim.core.mobsim.queuesim;

import org.matsim.ptproject.qsim.AgentCounterI;

/**Instantiable class that wraps around the deprecated "StaticAgentCounter" to provide for a transition in small steps
 * towards removing that class.
 * @author nagel
 *
 */
public class QueueAgentCounter implements AgentCounterI {


	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#decLiving()
	 */
	@Override
	public void decLiving() {
		AbstractSimulation.decLiving() ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#decLiving(int)
	 */
	@Override
	public void decLiving(int count) {
		AbstractSimulation.decLiving(count) ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#getLiving()
	 */
	@Override
	public int getLiving() {
		return AbstractSimulation.getLiving() ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#getLost()
	 */
	@Override
	public int getLost() {
		return AbstractSimulation.getLost() ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#incLiving()
	 */
	@Override
	public void incLiving() {
		AbstractSimulation.incLiving();
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#incLiving(int)
	 */
	@Override
	public void incLiving(int count) {
		AbstractSimulation.incLiving(count) ; 
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#incLost()
	 */
	@Override
	public void incLost() {
		AbstractSimulation.incLost();
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#incLost(int)
	 */
	@Override
	public void incLost(int count) {
		AbstractSimulation.incLost(count) ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#isLiving()
	 */
	@Override
	public boolean isLiving() {
		return AbstractSimulation.isLiving();
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#reset()
	 */
	@Override
	public void reset() {
		AbstractSimulation.reset() ;
	}

	/* (non-Javadoc)
	 * @see org.matsim.ptproject.qsim.AgentCounterI#setLiving(int)
	 */
	@Override
	public void setLiving(int count) {
		AbstractSimulation.setLiving(count) ;
	}
}
