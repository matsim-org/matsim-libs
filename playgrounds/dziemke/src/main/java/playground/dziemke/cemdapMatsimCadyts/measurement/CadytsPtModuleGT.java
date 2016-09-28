package playground.dziemke.cemdapMatsimCadyts.measurement;

import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.pt.CadytsPtContext;
import org.matsim.core.controler.AbstractModule;

/**
 * Created by GabrielT on 27.09.2016.
 */
public class CadytsPtModuleGT extends AbstractModule {

	@Override
	public void install() {
		bind(CadytsPtContext.class).asEagerSingleton();
		addControlerListenerBinding().to(CadytsPtContext.class);
		bind(CadytsBuilder.class).to(CadytsBuilderImplGT.class);
	}
}