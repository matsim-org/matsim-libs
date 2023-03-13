package org.matsim.core.mobsim.qsim;

import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

public interface AbortHandler extends QSimComponent {
	boolean handleAbort( MobsimAgent abortHandler );
}

