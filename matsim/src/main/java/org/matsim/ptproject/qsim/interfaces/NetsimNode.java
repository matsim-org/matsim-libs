package org.matsim.ptproject.qsim.interfaces;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimNetworkObject;
import org.matsim.utils.customize.Customizable;
import org.matsim.vis.snapshots.writers.VisNode;

public interface NetsimNode extends Customizable, VisNode, MatsimNetworkObject {

	@Override
	public Node getNode();

}