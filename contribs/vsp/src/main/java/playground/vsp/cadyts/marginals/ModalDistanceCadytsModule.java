package playground.vsp.cadyts.marginals;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;

public class ModalDistanceCadytsModule extends AbstractModule {

	public ModalDistanceCadytsModule() {
	}

	@Override
	public void install() {

		bind(TripEventHandler.class).in(Singleton.class);
		bind(ModalDistanceCadytsContext.class).in(Singleton.class);
		bind(ModalDistancePlansTranslator.class).in(Singleton.class);

		addControlerListenerBinding().to(ModalDistanceCadytsContext.class);
		addEventHandlerBinding().to(TripEventHandler.class);
	}
}
