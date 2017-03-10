package playground.sergioo.mixedTraffic2017;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;

import playground.sergioo.mixedTraffic2017.qsimmixed.DefaultQNetworkFactory;
import playground.sergioo.mixedTraffic2017.qsimmixed.QLanesNetworkFactory;
import playground.sergioo.mixedTraffic2017.qsimmixed.QNetworkFactory;
import playground.sergioo.mixedTraffic2017.qsimmixed.QNetsimEnginePlugin;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

public class QSimModule extends AbstractModule {
	@Inject Config config ;
	
	@Override
	protected void configure() {
		bind(Mobsim.class).toProvider(QSimProvider.class);
		if ( config.qsim().isUseLanes() ) { 
			bind(QNetworkFactory.class).to( QLanesNetworkFactory.class ) ;
			// yyyyyy why are the different traffic dynamics not working for the lanes?  kai, feb'17
		} else {
			switch( config.qsim().getTrafficDynamics() ) {
	//			case assignmentEmulating:
//					bind(QNetworkFactory.class).to( AssignmentEmulatingQLaneNetworkFactory.class ) ;
//					break;
				case queue:
				case withHoles:
				case kinematicWaves:
					bind(QNetworkFactory.class).to( DefaultQNetworkFactory.class ) ;
					break;
				default:
					throw new RuntimeException("not implemented") ;
					// yyyyyy means, I think, that the kinematic waves option does not work here.  ????  kai, feb'17
					// the case for kinematic waves was missing here, should work now. amit, feb'17
			}
		}
	}

	@SuppressWarnings("static-method")
	@Provides
	Collection<AbstractQSimPlugin> provideQSimPlugins(Config config1) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config1));
		plugins.add(new ActivityEnginePlugin(config1));
		plugins.add(new QNetsimEnginePlugin(config1));
		if (config1.network().isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config1));
		}
		if (config1.transit().isUseTransit()) {
			plugins.add(new TransitEnginePlugin(config1));
		}
		plugins.add(new TeleportationPlugin(config1));
		plugins.add(new PopulationPlugin(config1));
		return plugins;
	}
}
