package org.matsim.core.controler;

import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.controler.listener.ControlerListener;

public interface ControlerListenerManager extends MatsimManager {

	void addControlerListener(ControlerListener controlerListener);

}
