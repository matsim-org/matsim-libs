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

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JTextField;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.interfaces.OTFLiveServer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQuery.Type;
import org.matsim.vis.otfvis.interfaces.OTFQueryRemote;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.interfaces.OTFServer;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentEvents;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentId;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentPTBus;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentPlan;
import org.matsim.vis.otfvis.opengl.queries.QueryLinkById;
import org.matsim.vis.otfvis.opengl.queries.QueryLinkId;
import org.matsim.vis.otfvis.opengl.queries.QueryNodeById;
import org.matsim.vis.otfvis.opengl.queries.QuerySpinne;

/**
 * This class is only used with the "live" OTFVis. It represents th Query
 * issuing GUI element on the lower part of the screen. Alls queries are
 * initiated and managed here.
 * 
 * @author dstrippgen
 * 
 */
public class OTFQueryControl implements GLEventListener {

	private static final Logger log = Logger.getLogger(OTFQueryControl.class);

	private JTextField textField;

	private final OTFServer server;

	private final Map<OTFQueryRemote, OTFQueryResult> queryEntries = new HashMap<>();

	private final Vector<QueryEntry> queries = new Vector<>(Arrays
			.asList(new QueryEntry("agentPlan",
							"show the actual plan of an agent", QueryAgentPlan.class),
					new QueryEntry("agentEvents",
							"show the actual events of an agent",
							QueryAgentEvents.class), new QueryEntry(
							"agentPTBus",
							"highlight all buses of a given line",
							QueryAgentPTBus.class), new QueryEntry(
							"linkSpinneALL", "show Spinne of ALL traffic",
							QuerySpinne.class),
					new QueryEntry("linkById", "show link(s) by comma separated link id", QueryLinkById.class),
					new QueryEntry("nodeById", "show node(s) by comma separated node id", QueryNodeById.class)));

	private final OTFVisConfigGroup config;

	public OTFQueryControl(OTFServer server, final OTFVisConfigGroup config) {
		this.config = config;
		this.server = server;
	}

	@Override
	public void init(GLAutoDrawable glAutoDrawable) {

	}

	@Override
	public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {

	}

	synchronized void handleIdQuery(String id, String queryName) {
		AbstractQuery query = createQuery(queryName);
		query.setId(id);
		createQuery(query);
	}

	public void handleClick(Point2D.Double point, int mouseButton) {
		Rectangle2D.Double origRect = new Rectangle2D.Double(point.x, point.y,
				0, 0);
		// Only handle clicks with the main == zoom button
		if ((mouseButton == 1) || (mouseButton == 4))
			handleClick(origRect, mouseButton);
	}

	synchronized void removeQueries() {
		if(server.isLive()) {
			((OTFLiveServer) server).removeQueries();
		}
		for (OTFQueryResult query : this.queryEntries.values()) {
			query.remove();
		}
		this.queryEntries.clear();
		textField.setText("");
		((Component) OTFClientControl.getInstance().getMainOTFDrawer().getCanvas()).repaint();
	}

	@Override
	public void display(GLAutoDrawable glAutoDrawable) {
		for (OTFQueryResult queryResult : this.queryEntries.values()) {
			queryResult.draw(OTFClientControl.getInstance().getMainOTFDrawer());
		}
	}

	public void handleClick(Rectangle2D.Double origRect, int mouseButton) {
		if (mouseButton == 3) {
			removeQueries();
			((Component) OTFClientControl.getInstance().getMainOTFDrawer().getCanvas()).repaint();
		} else {
			String queryName = this.config.getQueryType();
			Type typeOfQuery = getTypeOfQuery(queryName);
			if (typeOfQuery == OTFQuery.Type.AGENT) {
				List<String> agentIds = resolveId(origRect);
				if ((agentIds != null) && (agentIds.size() != 0)) {
					log.debug("AgentId = " + agentIds);
					handleIdQuery(agentIds, queryName);
				} else {
					log.debug("No AgentId found!");
				}
			} else if (typeOfQuery == OTFQuery.Type.LINK) {
				QueryLinkId.Result linkIdQuery = (QueryLinkId.Result) createQuery(new QueryLinkId(origRect));
				if ((linkIdQuery != null) && (linkIdQuery.linkIds.size() != 0)) {
					log.debug("LinkId = " + linkIdQuery.linkIds);
					handleIdQuery(linkIdQuery.linkIds.values(), queryName);
				} else {
					log.debug("No LinkId found!");
				}
			}
		}
	}

	synchronized public void updateQueries() {
		for (Map.Entry<OTFQueryRemote, OTFQueryResult> queryItem : queryEntries.entrySet()) {
			OTFQueryResult queryResult = queryItem.getValue();
			if (queryResult.isAlive()) {
				OTFQueryRemote queryRemote = queryItem.getKey();
				queryResult.remove();
				OTFQueryResult newResult = queryRemote.query();
				queryItem.setValue(newResult);
			}
		}
	}

	private OTFQueryResult createQuery(AbstractQuery query) {
		OTFQueryRemote remoteQuery = doQuery(query);
		OTFQueryResult queryResult = remoteQuery.query();
		queryEntries.put(remoteQuery, queryResult);
		return queryResult;
	}

	private OTFQueryRemote doQuery(final AbstractQuery query) {
		return ((OTFLiveServer) server).answerQuery(query);
	}

	private void handleIdQuery(Collection<String> list, String queryName) {
		if (!this.config.isMultipleSelect()) {
			removeQueries();
		}
		StringBuilder infoText = new StringBuilder(textField.getText());
		for (String id : list) {
			if (infoText.length() != 0) {
				infoText.append(", ");
			}
			infoText.append(id);
			handleIdQuery(id, queryName);
		}
		textField.setText(infoText.toString());
		((Component) OTFClientControl.getInstance().getMainOTFDrawer().getCanvas()).repaint();
	}

	@SuppressWarnings("unchecked")
	AbstractQuery createQuery(String className) {
		try {
			Class<? extends AbstractQuery> classDefinition = (Class<? extends AbstractQuery>) Class
			.forName(className);
			return classDefinition.newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private Type getTypeOfQuery(String queryName) {
		OTFQuery query = createQuery(queryName);
		return query.getType();
	}

	private List<String> resolveId(Double origRect) {
		QueryAgentId.Result agentIdQuery = (QueryAgentId.Result) createQuery(new QueryAgentId(origRect));
		if ((agentIdQuery != null) && (agentIdQuery.agentIds.size() != 0)) {
			return agentIdQuery.agentIds;
		}
		return null;
	}

	public Vector<QueryEntry> getQueries() {
		return queries;
	}

	public void setQueryTextField(JTextField textField2) {
		this.textField = textField2;
	}

	@Override
	public void dispose(GLAutoDrawable glAutoDrawable) {

	}

}


