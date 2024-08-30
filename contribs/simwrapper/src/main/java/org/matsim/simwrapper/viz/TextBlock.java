package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The TextBlock Plug-In displays a markdown file on simwrapper.
 */
public class TextBlock extends Viz {

	/**
	 * The filepath containing the data.
	 */
	public String file;

	/**
	 * Content of the text block. Can be used instead of file to directly include content.
	 */
	public String content;

	public TextBlock() {
		super("text");
	}
}
