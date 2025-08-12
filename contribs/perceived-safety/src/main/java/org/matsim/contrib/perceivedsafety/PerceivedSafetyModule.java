package org.matsim.contrib.perceivedsafety;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

/**
 * Perceived Safety Module, which enables the perceived safety scoring function.
 */
public final class PerceivedSafetyModule extends AbstractModule {

	/**
	 * installs the module.
	 */
	public void install() {
//		add the scoring of perceived safety scores to the default matsim scoring
//		this adds additional terms to the scoring function instead of replacing it! -sm0325
		this.addEventHandlerBinding().to(PerceivedSafetyScoreEventsCreator.class);
		this.bind(AdditionalPerceivedSafetyLinkScore.class).to(AdditionalPerceivedSafetyLinkScoreDefaultImpl.class);

		this.installOverridingQSimModule(new AbstractQSimModule() {
			@Inject EventsManager events;
			@Inject Scenario scenario;
			@Override protected void configureQSim(){
//				TODO: what is the following needed for?? -sm0325
				final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
				bind(QNetworkFactory.class).toInstance(factory);}
		});
	}
}
