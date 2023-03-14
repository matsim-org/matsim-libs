package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;
// import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
// import org.matsim.contrib.bicycle.BicycleModule.ConsistencyCheck;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
// import org.matsim.core.controler.events.StartupEvent;
// import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
// import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultTurnAcceptanceLogic;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
// import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;
// import org.matsim.vehicles.VehicleType;
// import org.matsim.withinday.mobsim.WithinDayQSimModule;

public final class PsafeModule extends AbstractModule {
//	private static final Logger LOG = LogManager.getLogger(PsafeModule.class);
	
//	@Inject
//	private PsafeConfigGroup psafeConfigGroup;
	
	public void install() {
		
		// addTravelTimeBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelTime.class).in(Singleton.class);
		// addTravelDisutilityFactoryBinding(bicycleConfigGroup.getBicycleMode()).to(BicycleTravelDisutilityFactory.class).in(Singleton.class);

		// switch ( bicycleConfigGroup.getBicycleScoringType() ) {
		//	case legBased -> {
		//		this.addEventHandlerBinding().to( BicycleScoreEventsCreator.class );
		//		}
		//	case linkBased -> {
		bindScoringFunctionFactory().to(PsafeScoringFunctionFactory.class).in(Singleton.class);
		//}
		//	default -> throw new IllegalStateException( "Unexpected value: " + bicycleConfigGroup.getBicycleScoringType() );}

		// bind( BicycleLinkSpeedCalculator.class ).to( BicycleLinkSpeedCalculatorDefaultImpl.class ) ;

		// if (bicycleConfigGroup.isMotorizedInteraction()) {
		//	addMobsimListenerBinding().to(MotorizedInteractionEngine.class);}
		
	    // addControlerListenerBinding().to(ConsistencyCheck.class);
		
		this.installOverridingQSimModule(new AbstractQSimModule() {
			@Inject EventsManager events;
			@Inject Scenario scenario;
			@Override protected void configureQSim(){
				final ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
				bind( QNetworkFactory.class ).toInstance(factory );}
		});
	}
}	