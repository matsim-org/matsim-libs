package org.matsim.core.mobsim.qsim;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsModule;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueueModule;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.SimpleTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLanesNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class QSimModule extends AbstractModule {
	@Inject Config config ;
	
	private final boolean addDefaultQSimModules;
	
	public QSimModule() {
		this(true);
	}
	
	public QSimModule(boolean addDefaultQSimModules) {
		this.addDefaultQSimModules = addDefaultQSimModules;
	}
	
	@Override
	public void install() {
		install(new QSimComponentsModule());
		
		if (addDefaultQSimModules) {		
			getDefaultQSimModules().forEach(this::installQSimModule);
		}
		
		bind(Key.get(new TypeLiteral<List<AbstractQSimModule>>() {
		}, Names.named("overrides"))).toInstance(Collections.emptyList());
		
		bind(new TypeLiteral<Collection<AbstractQSimModule>>() {
		}).to(new TypeLiteral<Set<AbstractQSimModule>>() {
		});

		bind(Mobsim.class).toProvider(QSimProvider.class);

		if ( config.qsim().isUseLanes() ) {
			bind(QNetworkFactory.class).to( QLanesNetworkFactory.class ) ;
		} else {
			bind(QNetworkFactory.class).to( DefaultQNetworkFactory.class ) ;
		}
		
		if ( config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim() ) {
			bind( TransitStopHandlerFactory.class ).to( ComplexTransitStopHandlerFactory.class ) ;
		} else {
			// Explicit bindings are required, so although it may not be used, we need provide something.
			bind( TransitStopHandlerFactory.class ).to( SimpleTransitStopHandlerFactory.class );
		}
		// yy see MATSIM-756
	}

	static public Collection<AbstractQSimModule> getDefaultQSimModules() {
		return Arrays.asList(
				new MessageQueueModule(),  // defines "MessageQueueEngine"
				new ActivityEngineModule(), // defines "ActivityEngine"
				new QNetsimEngineModule(), // defines "NetsimEngine"
				new TeleportationModule(), // etc.
				new PopulationModule(),
				new NetworkChangeEventsModule(),
				new TransitEngineModule()
				);
	}
}
