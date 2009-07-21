package org.matsim.vis.otfvis.interfaces;

/**
 * OTFSettingsSaver is a minimal interface for storing 
 * the global settings.
 * 
 * @author dstrippgen
 *
 */
public interface OTFSettingsSaver {

	public void saveSettings();

	public void saveSettingsAs();

	public void readSettings();
	
}
