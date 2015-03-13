/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.artemc.analysis.postgresql;

import java.io.PushbackReader;
import java.util.List;

/**
 * @author fouriep writer for creating relational tables
 */
public abstract class TableWriter {
	String writerName = "";
	String tableName;
	int modfactor = 1;
	int lineCounter = 0;
	int batchSize = 1000;
	int pushBackSize = 200000;
	final StringBuilder sb = new StringBuilder();
	final List<PostgresqlColumnDefinition> columns;
	PushbackReader reader;
	private String comment;

	/**
	 * @param tableName
	 *            the name of the table to write to
	 * @param batchSize
	 *            the larger the batchsize, the more lines are sent in one
	 *            request.
	 * @param columns
	 *            column definitions, assuming only the 4 basic data types for
	 *            now.
	 */
    TableWriter(String tableName, int batchSize,
                List<PostgresqlColumnDefinition> columns) {
		this.tableName = tableName;
		this.batchSize = batchSize;
		this.columns = columns;
		this.pushBackSize = 0;
		for (PostgresqlColumnDefinition col : columns) {
			pushBackSize += col.type.size();
		}
		pushBackSize *= batchSize;
	}
 


	public abstract void init();
	public abstract void finish();

	public String getTableName() {
		return tableName;
	}

	public abstract void addLine(Object[] args);

	public void addComment(String format) {
		this.comment = comment;
		
	}
}
