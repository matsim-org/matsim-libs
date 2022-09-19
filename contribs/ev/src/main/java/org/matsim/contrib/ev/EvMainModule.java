package org.matsim.contrib.ev;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;

import java.util.List;

/**
 * The material in this class used to be in user code.  I did not understand why, and there was no explanation.  Now it is here.  As usual, if you do
 * not want to load all the functionality provided by the corresponding module (in this case {@link EvModule}), you will have to inline the module
 * contents and remove the material that you do not want.
 *
 * @author kainagel
 */
public final class EvMainModule extends AbstractModule{
	@Override
	public void install(){
		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( this.getConfig(), QSimComponentsConfigGroup.class );
		List<String> cmps = qsimComponentsConfig.getActiveComponents();
		cmps.add( EvModule.EV_COMPONENT );
		qsimComponentsConfig.setActiveComponents( cmps );

		addRoutingModuleBinding( TransportMode.car ).toProvider( new EvNetworkRoutingProvider( TransportMode.car ) );

		installQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				addMobsimScopeEventHandlerBinding().to( VehicleChargingHandler.class ).asEagerSingleton();
				// (leaving this out fails the events equality)
			}
		} );
	}
}
