package playground.mmoyo.analysis.comp;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.TransitSimulation.PlanRouter;

/**routes scenario with a defined cost calculations and increasing parameters coefficients**/
public class AllRouter {

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		/*
		Do not forget to set:
		-name of scenario
		-get ride of autos?
		-split plans?
		-cost coefficients
		-only plans inside the investigation area?
		-ATTENTION. There is not "cost calculator 2" anymore.
		*/
		
		String configFile = null;
		
		if (args.length>0){
			configFile = args[0];
		}else{
			configFile = "../playgrounds/mmoyo/output/comparison/Berlin/16plans/0config_5x_4plans.xml";
		}
		
		/*
		PTValues.scenarioName ="time100dist0CORRECTO";
		PTValues.routerCalculator = 3;
		PTValues.timeCoefficient = 1.0;
		PTValues.distanceCoefficient=0.0;
		//PlanRouter.main(new String[]{configFile});
		*/
		
		configFile = "../playgrounds/mmoyo/output/comparison/Berlin/16plans/0config_5x_4plans.xml";
		PTValues.scenarioName ="marcels";
		PTValues.routerCalculator = 1;
		PlanRouter.main(new String[]{configFile});
		
		
		/*
		PTValues.routerCalculator = 3;
		for (double x= 0; x<1.05; x = x + 0.05 ){
			PTValues.timeCoefficient=  Math.round(x*100)/100.0;
			PTValues.distanceCoefficient = Math.round((1-x)*100)/100.0;
			PTValues.scenarioName =  "_time" + PTValues.timeCoefficient + "_dist" + PTValues.distanceCoefficient;
			System.out.println(PTValues.scenarioName);
			
			//PlanRouter.main(new String[]{configFile});
		}
		*/
	}
}

/*
	//shut down ms-windows
	Runtime runtime = Runtime.getRuntime();
	runtime.exec("shutdown -s -t 60 -f");  
*/
