package playground.wrashid.msimoni.roadpricing;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;

public class Main {

	public static void main(String[] args) {
		Controler controler=new Controler(args);
        controler.setModules(new ControlerDefaultsWithRoadPricingModule());
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}
	
}
