package org.matsim.contrib.dvrp.passenger;

import static org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule.PassengerEngineType.DEFAULT;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

/**
 * dvrp needs {@link PassengerHandler} bound to something.  It is first bound to a {@link PassengerEngine} so it can also do stuff on its own.  Then,
 * {@link PassengerEngine} is bound to one of the existing implementations.
 */
public class PassengerEngineQSimModule extends AbstractDvrpModeQSimModule {
	public enum PassengerEngineType {
		DEFAULT, WITH_PREBOOKING, TELEPORTING, WITH_GROUPS
	}

	private final PassengerEngineType type;

	public PassengerEngineQSimModule(String mode) {
		this(mode, DEFAULT);
	}

	public PassengerEngineQSimModule(String mode, PassengerEngineType type) {
		super(mode);
		this.type = type;
	}

	@Override
	protected void configureQSim() {
		bindModal(PassengerHandler.class).to(modalKey(PassengerEngine.class));
		// (PassengerEngine is a more powerful interface.)

		addMobsimScopeEventHandlerBinding().to(modalKey(PassengerEngine.class));

		switch( type ){
			case DEFAULT -> {
				addModalComponent( PassengerEngine.class, DefaultPassengerEngine.createProvider( getMode() ) );
				return;
			}
			case WITH_PREBOOKING -> {
				addModalComponent( PassengerEngine.class, PassengerEngineWithPrebooking.createProvider( getMode() ) );
				return;
			}
			case TELEPORTING -> {
				addModalComponent( PassengerEngine.class, TeleportingPassengerEngine.createProvider( getMode() ) );
				return;
			}
			case WITH_GROUPS -> {
				addModalComponent( PassengerEngine.class, GroupPassengerEngine.createProvider( getMode() ) );
				return;
			}
			default -> throw new IllegalStateException( "Type: " + type + " is not supported" );
		}
	}
}
