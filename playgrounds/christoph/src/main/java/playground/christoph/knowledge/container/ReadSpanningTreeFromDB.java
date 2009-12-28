/* *********************************************************************** *
 * project: org.matsim.*
 * ReadSpanningTreeFromDB.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.knowledge.container;

import java.sql.ResultSet;

import org.matsim.api.core.v01.network.Node;

import playground.christoph.knowledge.container.dbtools.DBConnectionTool;
import playground.christoph.tools.Zipper;

/*
 * Reads the Spanning Trees that are created by using
 * the Dijsktra Node Selection Tool from a DataBase.
 * 
 * The DataBase configuration has to be set in the DBConnectionTool.
 */
public class ReadSpanningTreeFromDB {

	/*
	 * Should the byte Arrays be compressed? Will save about 10% memory but
	 * increases the calculation effort quite a lot.
	 */
	private boolean useCompression = false;
	
	private DBConnectionTool dbConnectionTool;
	private Zipper zipper;
	
	public ReadSpanningTreeFromDB()
	{
		this.dbConnectionTool = new DBConnectionTool();
		this.zipper = new Zipper();

	}
	
	public DBConnectionTool getDBConnectionTool()
	{
		return this.dbConnectionTool;
	}
	
	public void setDBConnectionTool(DBConnectionTool tool)
	{
		this.dbConnectionTool = tool;
	}
	
	public byte[] readFromDB(Node node, String tableName)
	{	
		ResultSet rs;
		
		dbConnectionTool.connect();
		rs = dbConnectionTool.executeQuery("SELECT * FROM " + tableName + " WHERE NodeId='" + node.getId() + "'");
		dbConnectionTool.disconnect();
				
		try 
		{	
			while (rs.next())
			{					
				byte[] bytes = rs.getBytes("SpanningTree");
				
				if (this.useCompression)
				{
					bytes = this.zipper.decompress(bytes);
				}
				
				return bytes;
			}
		}
		catch (Exception e)
		{		
			e.printStackTrace();
		}
		
		return null;
	}
}
