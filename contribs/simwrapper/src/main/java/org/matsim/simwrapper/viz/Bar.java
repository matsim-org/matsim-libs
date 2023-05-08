package org.matsim.simwrapper.viz;

import java.util.ArrayList;
import java.util.List;

public class Bar extends Chart{

	/**
	 * true/false for bar charts, whether to stack multiple bars
	 */
	public boolean stacked;

	public List<String> ignoredColumns = new ArrayList<>();

	public Bar() {
		super("bar");
	}
}
