package playground.rost.controller.gui.helpers.progressinformation;

import java.util.List;

public interface ProgressInformationProvider {
	
	/**
	 * @param key
	 * @return value corresponding to the key
	 */
	public String getProgressInformation(String key);
	
	public List<String> getListOfKeys();
	
	public boolean isFinished();
	public String getTitle();
}
