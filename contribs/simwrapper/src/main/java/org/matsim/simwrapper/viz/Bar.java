package org.matsim.simwrapper.viz;

public class Bar extends Chart{

	/**
	 * true/false for bar charts, whether to stack multiple bars
	 */
	public boolean stacked;

	public String ignoredColumns;

	public Bar() {
		super("bar");
	}
}
