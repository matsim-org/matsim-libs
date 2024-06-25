package org.matsim.simwrapper;


/**
 * Header section of a {@link Dashboard}.
 */
public final class Header {

	/**
	 * Text to be displayed in the tab.
	 */
	public String tab;
	/**
	 * Title of the dashboard.
	 */
	public String title;
	public String description;

	/**
	 * Enable dashboard to fill the whole screen.
	 */
	public Boolean fullScreen;

	/**
	 * Set the dashboard to appear only when a certain file / directory is present.
	 */
	public String triggerPattern;

}
