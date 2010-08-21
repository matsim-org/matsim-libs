package playground.wrashid.lib;

import org.matsim.core.controler.Controler;

public class GlobalRegistry {

	public static Controler controler = null;
	public static boolean isTestingMode = false;

	public static boolean doPrintGraficDataToConsole = false;

	public static Double readDoubleFromConfig(String moduleName, String paramName) {
		String doubleValue = null;

		try {
			doubleValue = controler.getConfig().getParam(moduleName, paramName);
		} catch (Exception e) {
			return null;
		}

		return new Double(doubleValue);

	}
}
