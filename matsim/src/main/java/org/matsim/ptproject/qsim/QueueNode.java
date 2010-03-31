package org.matsim.ptproject.qsim;

import org.matsim.api.core.v01.network.Node;

/**
 * 
 * @author mzilske
 * @deprecated this class is only here for backwards compatibility of  OTFVis
 */
@Deprecated
public class QueueNode extends QNode {

	public QueueNode(Node n, QSimEngine engine) {
		super(n, engine);
	}

}
