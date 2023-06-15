package org.matsim.simwrapper.viz;

import java.util.List;

/**
 * Creates a Bar Plot for simwrapper.
 */
public class Bar extends Chart {

	/**
	 * true/false for bar charts, whether to stack multiple bars.
	 */
	public Boolean stacked;

	/**
	 * Defines als columns that should be not displayed.
	 */
	public List<String> ignoreColumns;

	public Bar() {
		super("bar");
	}
}
