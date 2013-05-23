package playground.hkluepfel.grips.scenariomanager;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.Constants.ModuleType;

public class ComplexODSelector extends AbstractModule {

	public ComplexODSelector(String title, ModuleType moduleType,
			Controller controller) {
		super(title, moduleType, controller);
		// TODO Auto-generated constructor stub
	}

	public ComplexODSelector(Controller controller) {
		super(controller.getLocale().modulePopAreaSelector(), Constants.ModuleType.COMPLEXOD, controller);
	}

}
