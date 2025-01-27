package org.matsim.simwrapper.viz;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The Table Plug-In generates a table from a .csv file.
 */
public class Table extends Viz {

	/**
	 * The filepath containing the data.
	 */
	@JsonProperty(required = true)
	public String dataset;

	/**
	 * Array to set the alignment of the columns. Possible values are left, center, right.
	 */
	public String[] alignment;

	/**
	 * This option could be used to define the style. Predefined styles are e.g. default, topsheet
	 */
	public String style;

	/**
	 * This option could be used to filter columns. This option adds a filter mask to each column. The default setting is false.
	 */
	public Boolean enableFilter;

	/**
	 * Array of strings. List of column names that should be ignored.
	 */
	public List<String> hide;

	/**
	 * Array of strings. List of column names that should be displayed. If the hide and show option are in the .yaml file the hide option will be ignored
	 */
	public List<String> show;

	/**
	 * This option defines whether the whole table should be displayed or if there are several pages. The default setting is false.
	 */
	public Boolean showAllRows;

	public Table() {
		super("csv");
	}
}
