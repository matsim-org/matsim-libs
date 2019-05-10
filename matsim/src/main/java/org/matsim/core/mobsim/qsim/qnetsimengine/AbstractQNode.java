package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.matsim.api.core.v01.network.Node;

/**
 * {@link QNodeI} is the interface; this is an abstract class that contains implementation
 * of non-traffic related "infrastructure", primarily (de)activation.
 *
 */

abstract class AbstractQNode implements QNodeI {

	// necessary if Nodes are (de)activated
	private NetElementActivationRegistry activator = null;

	/*
	 * This needs to be atomic since this allows us to ensure that an node which is
	 * already active is not activated again. This could happen if multiple thread call
	 * activateNode() concurrently.
	 * cdobler, sep'14
	 */
	private final AtomicBoolean active = new AtomicBoolean(false);

	// for Customizable
	private final Map<String, Object> customAttributes = new HashMap<>();
	
	final Node node;

	
	
	AbstractQNode(final Node n){
		this.node = n;
	}
	
	
	@Override
	public final Node getNode() {
		return this.node;
	}
	
	
	/**
	 * This method is called from QueueWithBuffer.addToBuffer(...) which is triggered at 
	 * some placed, but always initially by a QLink's doSomStep(...) method. I.e. QNodes
	 * are only activated while moveNodes(...) is performed. However, multiple threads
	 * could try to activate the same node at a time, therefore this has to be thread-safe.
	 * cdobler, sep'14 
	 */
	/*package*/ final void activateNode() {
		// yyyy I cannot say if this needs to be in QNodeI or not.  The mechanics of this are tricky to implement, so it would 
		// not be a stable/robust API.  kai, jul'17
		
		/*
		 * this.active.compareAndSet(boolean expected, boolean update)
		 * We expect the value to be false, i.e. the node is de-activated. If this is
		 * true, the value is changed to true and the activator is informed.
		 */
		if (this.active.compareAndSet(false, true)) {
			this.activator.registerNodeAsActive(this);
		}
	}
	
	
	final boolean isActive() {
		// yyyy I cannot say if this needs to be in QNodeI or not.  The mechanics of this are tricky to implement, so it would 
		// not be a stable/robust API.  kai, jul'17
		
		return this.active.get();
	}
	
	final void setActive(boolean active) {
		this.active.set(active);
	}


	
	/**
	 * The ParallelQSim replaces the activator with the QSimEngineRunner 
	 * that handles this node.
	 */
	/*package*/ final void setNetElementActivationRegistry(NetElementActivationRegistry activator) {
		// yyyy I cannot say if this needs to be in QNodeI or not.  The mechanics of this are tricky to implement, so it would 
		// not be a stable/robust API.  kai, jul'17
		
		this.activator = activator;
	}
	
	@Override
	public final Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}
}
