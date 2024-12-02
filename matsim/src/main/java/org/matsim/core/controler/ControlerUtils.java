package org.matsim.core.controler;

import com.google.inject.Injector;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;

/**
 * @deprecated -- use class and methods with double l.
 */
@Deprecated
public class ControlerUtils {
	private ControlerUtils(){} // namespace only; do not instantiate

	/**
	 * @param scenario
	 * @return
	 * @deprecated -- use controller methods with double l
	 */
	@Deprecated
	public Controler createControler( Scenario scenario ) {
		return (Controler) ControllerUtils.createController( scenario );
	}

	public Injector createAdhocInjector( Config config, Scenario scenario ) {
		return ControllerUtils.createAdhocInjector( config, scenario );
	}
	public Injector createAdhocInjector( Scenario scenario ) {
		return ControllerUtils.createAdhocInjector( scenario );
	}
}
