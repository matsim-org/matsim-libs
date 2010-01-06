/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQueryControlBar.java
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

package org.matsim.vis.otfvis.gui;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryHandler;
import org.matsim.vis.otfvis.interfaces.OTFQuery.Type;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentEvents;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentId;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentPTBus;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentPlan;
import org.matsim.vis.otfvis.opengl.queries.QueryLinkId;
import org.matsim.vis.otfvis.opengl.queries.QuerySpinne;
import org.matsim.vis.otfvis.opengl.queries.QuerySpinneNOW;

/**
 * This class is only used with the "live" OTFVis. It represents th Query issuing GUI element on the lower part of the screen.
 * Alls queries are initiated and managed here.
 * 
 * @author dstrippgen
 *
 */
public class OTFQueryControl implements OTFQueryHandler {
  
	private static final Logger log = Logger.getLogger(OTFQueryControl.class);
  
	private JTextField textField;
  
	private IdResolver agentIdResolver = null;

	private final OTFHostControlBar hostControlBar;
	private final List<OTFQuery> queryItems = new ArrayList<OTFQuery>();

	private final Vector<QueryEntry> queries = new Vector<QueryEntry>(Arrays.asList(
			new QueryEntry("agentPlan", "show the actual plan of an agent", QueryAgentPlan.class),
			new QueryEntry("agentEvents", "show the actual events of an agent", QueryAgentEvents.class),
			new QueryEntry("agentPTBus", "highlight all buses of a given line", QueryAgentPTBus.class),
			new QueryEntry("linkSpinneALL", "show Spinne of ALL traffic", QuerySpinne.class),
			new QueryEntry("linkSpinneNOW", "show Spinne of all veh on the link NOW", QuerySpinneNOW.class)
	));

	private final OTFVisConfig config; 


	public OTFQueryControl(OTFHostControlBar handler, final OTFVisConfig config) {
		this.config = config;
		if(agentIdResolver == null) {
		  agentIdResolver  = new myIdResolver();
		}
		this.hostControlBar = handler;
		this.config.setQueryType(queries.get(0).clazz.getCanonicalName());
	}

	synchronized public void handleIdQuery(String id, String queryName) {
		OTFQuery query = createQuery(queryName);
		query.setId(id);
		query = hostControlBar.doQuery(query);
		this.queryItems.add(query);
	}

	public void handleIdQuery(Collection<String> list, String queryName) {
		if (!this.config.isMultipleSelect()) {
			removeQueries();
		}
		StringBuilder infoText = new StringBuilder(textField.getText());
		for(String id : list) {
			if (infoText.length() != 0) {
				infoText.append(", ");
			}
			infoText.append(id);
			handleIdQuery(id, queryName);	
		}

		textField.setText(infoText.toString());
		hostControlBar.redrawDrawers();
	}

	public OTFQuery handleQuery(OTFQuery query) {
		return hostControlBar.doQuery(query);
	}

	public void handleClick(String viewId, Point2D.Double point, int mouseButton) {
		Rectangle2D.Double origRect = new Rectangle2D.Double(point.x, point.y ,0,0);
		// Only handle clicks with the main == zoom button
		if ((mouseButton==1) || (mouseButton==4)) handleClick(viewId, origRect, mouseButton);
	}

	protected OTFQuery createQuery(String className) {
		try {
			Class classDefinition = Class.forName(className);
			return (OTFQuery) classDefinition.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public void handleClick(String viewId, Rectangle2D.Double origRect, int mouseButton) {
		if (mouseButton == 3) {
			removeQueries();
			hostControlBar.redrawDrawers();
		}
		else {
			String queryName = this.config.getQueryType();
			Type typeOfQuery = getTypeOfQuery(queryName);

			if (typeOfQuery == OTFQuery.Type.AGENT) {
				List<String> agentIds = agentIdResolver.resolveId(origRect);
				if ((agentIds != null) && (agentIds.size() != 0)) {
					log.debug("AgentId = " + agentIds);
					handleIdQuery(agentIds, queryName);
				} else {
					log.debug("No AgentId found!");
				}
			} else if (typeOfQuery == OTFQuery.Type.LINK) {
				QueryLinkId linkIdQuery = (QueryLinkId)hostControlBar.doQuery(new QueryLinkId(origRect));
				if ((linkIdQuery != null) && (linkIdQuery.linkIds.size() != 0)) {
					log.debug("LinkId = " + linkIdQuery.linkIds);
					handleIdQuery(linkIdQuery.linkIds.values(), queryName);
				} else {
					log.debug("No LinkId found!");
				}
			}
		}
	}

	private Type getTypeOfQuery(String queryName) {
		OTFQuery query = createQuery(queryName);
		Type typeOfQuery = query.getType();
		return typeOfQuery;
	}

	synchronized public void addQuery(OTFQuery query) {
		this.queryItems.add(query);
		hostControlBar.redrawDrawers();
	}

	synchronized public void removeQueries(){
		for(OTFQuery query : this.queryItems){
			if(query != null) {
			  query.remove();
			}
		}
		this.queryItems.clear();
		textField.setText("");

		hostControlBar.redrawDrawers();
	}

	synchronized public void drawQueries(OTFDrawer drawer) {
		for(OTFQuery query : this.queryItems){
			query.draw(drawer);
		}
	}

	synchronized public void updateQueries() {
		List<OTFQuery> replacedItems = new ArrayList<OTFQuery>();

		Iterator<OTFQuery> iter = queryItems.iterator();
		while(iter.hasNext()) {
			OTFQuery query = iter.next();
			if (query.isAlive()) {
				replacedItems.add(handleQuery(query));
				iter.remove();
			}
		}
		queryItems.addAll(replacedItems);
	}


	public interface IdResolver {
		List<String> resolveId(Double origRect);
	}

	public class myIdResolver implements IdResolver {
		public List<String> resolveId(Double origRect) {
			QueryAgentId agentIdQuery = (QueryAgentId)hostControlBar.doQuery( new QueryAgentId(origRect));
			if ((agentIdQuery != null) && (agentIdQuery.agentIds.size() != 0)) {
				return agentIdQuery.agentIds; 
			}
			return null;
		}
	}

  
  public Vector<QueryEntry> getQueries() {
    return queries;
  }

  public void setQueryTextField(JTextField textField2) {
    this.textField = textField2;
  }
}

 class QueryEntry {
   public String shortName;
   public String toolTip;
   public Class<? extends OTFQuery> clazz;
   
   public QueryEntry(String string, String string2, Class<? extends OTFQuery> class1) {
    this.shortName = string;
    this.toolTip = string2;
    this.clazz = class1;
  }
  @Override
  public String toString() { 
    return shortName;
  }

}

