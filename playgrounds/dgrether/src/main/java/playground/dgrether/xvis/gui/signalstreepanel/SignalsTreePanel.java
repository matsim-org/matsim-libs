/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.gui.signalstreepanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalSystem;

import playground.dgrether.xvis.control.XVisControl;
import playground.dgrether.xvis.control.events.ShowPanelEvent;
import playground.dgrether.xvis.control.events.SignalSystemSelectionEvent;
import playground.dgrether.xvis.gui.PanelManager;
import playground.dgrether.xvis.gui.signalplanpanel.SignalPlanPanel;


/**
 * @author dgrether
 *
 */
public class SignalsTreePanel extends JPanel implements TreeSelectionListener {

	private static final Logger log = Logger.getLogger(SignalsTreePanel.class);
	
	static final String PLAN_NODE_PREFIX = "Plan Id: ";
	
	private SignalsData signalsData;

	private JTree tree;

	static final String SIGNAL_SYSTEM_PREFIX = "Signal System Id:";

	public SignalsTreePanel(SignalsData signalsData) {
		this.signalsData = signalsData;
		this.initGui();
	}

	private void initGui() {
		BorderLayout layout = new BorderLayout();
		this.setLayout(layout);
		this.initNorthPanel();
		this.setPreferredSize(new Dimension(300,300));
		this.setMinimumSize(new Dimension(300,300));
		
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("SignalSystems");
		createNodes(top);
		
		this.tree = new JTree(top);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		tree.setCellRenderer(new SignalControlChildRenderer());
		this.add(tree, BorderLayout.CENTER);
	}

	private void initNorthPanel() {
//		SpringLayout layout = new SpringLayout();
		FlowLayout layout = new FlowLayout();
		JPanel panel = new JPanel(layout);
		JLabel label = new JLabel("Signal Control");
		panel.add(label);
		JButton minusButton = new JButton();
		panel.add(minusButton);
		minusButton.setAction(new AbstractAction("-") {
			@Override
			public void actionPerformed(ActionEvent e){
				collapseAll();
			}

		});
		JButton plusButton = new JButton();
		panel.add(plusButton);
		plusButton.setAction(new AbstractAction("+") {
			@Override
			public void actionPerformed(ActionEvent e){
				expandAll();
			}

		});
		
//		int xPad = XVisControl.getInstance().getLayoutPreferences().getDefaultXPadding();
//		int yPad = XVisControl.getInstance().getLayoutPreferences().getDefaultYPadding();
//		SpringUtilities.makeCompactGrid(panel, 1, this.getComponentCount(), xPad, yPad, xPad, yPad);

		this.add(panel, BorderLayout.NORTH);
	}

	
	public void expandAll() {
    int row = 0;
    while (row < this.tree.getRowCount()) {
      tree.expandRow(row);
      row++;
      }
    }

	public void collapseAll() {
    int row = tree.getRowCount() - 1;
    while (row >= 0) {
      tree.collapseRow(row);
      row--;
      }
    }

	
	
	private void createNodes(DefaultMutableTreeNode top) {
		if (this.signalsData.getSignalControlData() != null){
			top.add(new SignalControlChild(this.signalsData.getSignalControlData()));
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
		if (node == null)
			return;
		
		log.debug("Node selected: " + node);
		if (node.getUserObject() instanceof String){
			String nodeString = (String) node.getUserObject();
			if (nodeString.startsWith(SIGNAL_SYSTEM_PREFIX)){ // this is a signal system node
				Id signalSystemId = this.getSignalSystemIdFromNodeString(nodeString);
				SignalSystemSelectionEvent selectionEvent = new SignalSystemSelectionEvent(true);
				selectionEvent.addSignalSystemId(signalSystemId);
				XVisControl.getInstance().getControlEventsManager().fireSelectionEvent(selectionEvent);
			}
		}
		else if (node.getUserObject() instanceof SignalPlanData){
			DefaultMutableTreeNode systemNode = (DefaultMutableTreeNode) e.getPath().getPathComponent(2);
			String nodeString = (String) systemNode.getUserObject();
			if (!nodeString.startsWith(SIGNAL_SYSTEM_PREFIX)){
//				throw new RuntimeException(nodeString);
				log.error("nodeString is not correct!");
			}
			Id signalSystemId = this.getSignalSystemIdFromNodeString(nodeString);
			SignalSystemSelectionEvent selectionEvent = new SignalSystemSelectionEvent(true);
			selectionEvent.addSignalSystemId(signalSystemId);
			XVisControl.getInstance().getControlEventsManager().fireSelectionEvent(selectionEvent);
			SignalPlanData plan = (SignalPlanData) node.getUserObject();
			XVisControl.getInstance().getControlEventsManager().fireShowPanelEvent(new ShowPanelEvent(new SignalPlanPanel(signalSystemId, plan), PanelManager.Area.RIGHT));
		}
		
	}
	
	private Id<SignalSystem> getSignalSystemIdFromNodeString(String nodeString){
		Id<SignalSystem> signalSystemId = Id.create(nodeString.substring(SIGNAL_SYSTEM_PREFIX.length()), SignalSystem.class);
		log.error("Node string: " + nodeString + " id " + signalSystemId);
		return signalSystemId;
	}
	
}




class SignalControlChildRenderer extends DefaultTreeCellRenderer {
	
	@Override
	public Component getTreeCellRendererComponent(JTree tree,
      Object value,
      boolean sel,
      boolean expanded,
      boolean leaf,
      int row,
      boolean hasFocus) {
		
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node.getUserObject() instanceof SignalPlanData){
			String id = "Plan Id: " + ((SignalPlanData)node.getUserObject()).getId().toString();
			return new JLabel(id);
		}
		return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		
	}

}

class SignalControlChild extends DefaultMutableTreeNode implements MutableTreeNode {

	public SignalControlChild(SignalControlData signalControlData) {
		super("Signal Control");
		for (SignalSystemControllerData controllerData : signalControlData.getSignalSystemControllerDataBySystemId().values()){
			DefaultMutableTreeNode systemNode = new DefaultMutableTreeNode(SignalsTreePanel.SIGNAL_SYSTEM_PREFIX + controllerData.getSignalSystemId());
			this.add(systemNode);
			if (controllerData.getSignalPlanData() != null){
				for (SignalPlanData plan : controllerData.getSignalPlanData().values()){
					DefaultMutableTreeNode planNode = new DefaultMutableTreeNode(plan);
					systemNode.add(planNode);
					if (plan.getCycleTime() != null){
						planNode.add(new DefaultMutableTreeNode("Cycle " + plan.getCycleTime().toString()));
					}
				}
			}
		}
	}

	
	
}
