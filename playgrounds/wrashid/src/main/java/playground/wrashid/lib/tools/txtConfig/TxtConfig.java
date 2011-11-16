package playground.wrashid.lib.tools.txtConfig;

import java.util.HashMap;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class TxtConfig {

	HashMap<String, String> parameterValues;
	
	public TxtConfig(String fileName){
		parameterValues=new HashMap<String, String>();
		StringMatrix stringMatrix = GeneralLib.readStringMatrix(fileName, "\t");
		
		for (int i=0;i<stringMatrix.getNumberOfRows();i++){
			parameterValues.put(stringMatrix.getString(i, 0), stringMatrix.getString(i, 1));
		}
	}
	
	public String getParameterValue(String key){
		return parameterValues.get(key);
	}
	
}
