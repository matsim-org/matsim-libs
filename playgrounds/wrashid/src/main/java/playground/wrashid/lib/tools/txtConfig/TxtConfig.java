package playground.wrashid.lib.tools.txtConfig;

import java.io.File;
import java.util.HashMap;

import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;


/**
 * usage: put tab separated key, value pairs into a file and read them
 * afterwards.
 * 
 * keys can be used in other keys in the form of #key# and will be substituted,
 * up to level 2.
 * 
 * This means, that even if a variable contains another substitution variable,
 * which contains another variable to be substitued, that still should work.
 * 
 * 
 * - no empty lines allowed!
 * 
 * reserved keywords, that can be used in the file @parentDirectory@ which refers to
 * the parent directory of the config file
 * 
 * 
 * @author wrashid
 * 
 */
public class TxtConfig {

	HashMap<String, String> parameterValues;
	String parentDirectory;

	public TxtConfig(String fileName) {
		File file = new File(fileName);
		parentDirectory = file.getParentFile().toString().replace("\\", "/");

		parameterValues = new HashMap<String, String>();
		Matrix stringMatrix = GeneralLib.readStringMatrix(fileName, "\t");

		for (int i = 0; i < stringMatrix.getNumberOfRows(); i++) {
			parameterValues.put(stringMatrix.getString(i, 0), stringMatrix.getString(i, 1));
		}	
		
		preprocessReservedKeyWords();

		processSubstituions();
		processSubstituions(); // allow 2 levels of substitutions

		executeCommands();
	}
	
	

	private void executeCommands() {
		for (String key : parameterValues.keySet()) {
			String value = parameterValues.get(key);
			if (key.equalsIgnoreCase("!mkdir")){
				new File(value).mkdir();
			}
		}
	}



	private void preprocessReservedKeyWords() {
		for (String key : parameterValues.keySet()) {
			String value = parameterValues.get(key);
			value = value.replaceAll("@parentDirectory@", parentDirectory);
			parameterValues.put(key, value);
		}
	}

	private void processSubstituions() {
		for (String mainKey : parameterValues.keySet()) {
			String value = parameterValues.get(mainKey);

			for (String substituionKey : parameterValues.keySet()) {
				value = value.replaceAll("#" + substituionKey + "#", parameterValues.get(substituionKey));
			}

			parameterValues.put(mainKey, value);
		}
	}

	/**
	 * returns null, if value does not exist.
	 * 
	 * @param key
	 * @return
	 */
	public String getParameterValue(String key) {
		return parameterValues.get(key);
	}

	public int getIntParameter(String key) {
		String parameterValue = getParameterValue(key);
		if (parameterValue == null) {
			DebugLib.stopSystemAndReportInconsistency("key missing: " + key);
		}

		return Integer.parseInt(parameterValue);
	}

}
