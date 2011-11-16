package playground.wrashid.lib.tools.txtConfig;

import java.util.HashMap;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;
/**
 * usage: put tab separated key, value pairs into a file and read them afterwards.
 * 
 * keys can be used in other keys in the form of #key# and will be substituted, up to level 2.
 * 
 * 
 * 
 * @author wrashid
 *
 */
public class TxtConfig {

	HashMap<String, String> parameterValues;

	public TxtConfig(String fileName) {
		parameterValues = new HashMap<String, String>();
		StringMatrix stringMatrix = GeneralLib.readStringMatrix(fileName, "\t");

		for (int i = 0; i < stringMatrix.getNumberOfRows(); i++) {
			parameterValues.put(stringMatrix.getString(i, 0), stringMatrix.getString(i, 1));
		}

		processSubstituions();
		processSubstituions(); // allow several levels of substitutions
	}

	private void processSubstituions() {
		for (String mainKey : parameterValues.keySet()) {
			String value = parameterValues.get(mainKey);

			
			for (String substituionKey : parameterValues.keySet()) {
				value=value.replaceAll("#" + substituionKey + "#", parameterValues.get(substituionKey));
			}
			
			parameterValues.put(mainKey,value);
		}
	}

	/**
	 * returns null, if value does not exist.
	 * @param key
	 * @return
	 */
	public String getParameterValue(String key) {
		return parameterValues.get(key);
	}

}
