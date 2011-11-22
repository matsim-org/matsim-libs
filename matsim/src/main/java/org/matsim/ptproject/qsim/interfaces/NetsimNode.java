package org.matsim.ptproject.qsim.interfaces;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimNetworkObject;
import org.matsim.utils.customize.Customizable;

public interface NetsimNode extends Customizable, MatsimNetworkObject {

	public Node getNode();

}