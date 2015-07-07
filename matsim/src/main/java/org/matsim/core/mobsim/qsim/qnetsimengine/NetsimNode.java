package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimNetworkObject;

public interface NetsimNode extends Customizable, MatsimNetworkObject {

	Node getNode();

}