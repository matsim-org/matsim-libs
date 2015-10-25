package vind;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

@Deprecated
public class MyBindings {

	private MyBindings() {
	}
	
	// nur eine Gedankenstütze
	public static void bind(final Controler controler, final Object o) {
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				binder().requestInjection(o);
			}
		});
	}

}
