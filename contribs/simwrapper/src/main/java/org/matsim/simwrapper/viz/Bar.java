package org.matsim.simwrapper.viz;

public class Bar extends Chart{

	/**
	 * true/false for bar charts, whether to stack multiple bars
	 */
	public String stacked; // TODO: should be boolean ?

	public Bar() {
		super("bar");
	}
}
