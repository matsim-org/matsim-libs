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
package playground.dgrether.xvis.gui.signalplanpanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;

import playground.dgrether.xvis.control.XVisControl;
import playground.dgrether.xvis.control.events.SignalGroupsSelectionEvent;

/**
 * @author dgrether
 *
 */
public class SignalPlanPanel extends JPanel implements TableCellRenderer, ListSelectionListener {

	private static final Logger log = Logger.getLogger(SignalPlanPanel.class);

	private SignalPlanData signalPlan;

	final static Color textColor = new Color(0, 0, 0);
	final static Color greenColor = new Color(30, 200, 30);

	private final int preferedRowHeight = 40;
	private final int column0PreferedWidth = 100;
	private int preferedSettingsWidth = 300;
	
	private JTable table;

	private Id signalSystemId;

	public SignalPlanPanel(Id signalSystemId, SignalPlanData signalPlan){
		this.signalPlan = signalPlan;
		this.signalSystemId = signalSystemId;
		this.preferedSettingsWidth = signalPlan.getCycleTime() * 3;
		this.initGUI();
		log.debug("SignalPlanPanel initialized!");
	}
	
	private void initGUI(){
		this.setLayout(new BorderLayout());
		String headerLabelText = "Signal System Id: " + this.signalSystemId + " Plan Id: " + this.signalPlan.getId();
		JLabel headerLabel = new JLabel(headerLabelText);
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
		this.add(headerLabel, BorderLayout.NORTH);
		this.table = new JTable() {
			 @Override
			public TableCellRenderer getCellRenderer(int row, int column) {
	        if (column == 1) {
	            return SignalPlanPanel.this;
	        }
	        return super.getCellRenderer(row, column);
	    }
		};
		this.table.setModel(new SignalPlanTableModel(this.signalPlan));
		this.table.getColumnModel().getColumn(0).setPreferredWidth(column0PreferedWidth);
		this.table.getColumnModel().getColumn(1).setPreferredWidth(preferedSettingsWidth);
		this.table.getColumnModel().getColumn(1).setMinWidth(preferedSettingsWidth);
		this.table.getColumnModel().getColumn(1).setMaxWidth(preferedSettingsWidth);
		this.table.setRowHeight(preferedRowHeight);
		this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.table.getSelectionModel().addListSelectionListener(this);
		JScrollPane scrollPane = new JScrollPane(this.table);
		this.add(scrollPane, BorderLayout.CENTER);
	}
	

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
//		log.error("Painting Signal Group Settings...");
		return new SignalGroupSettingsRow((SignalGroupSettingsData)value, this.signalPlan.getCycleTime(), new Dimension(preferedSettingsWidth, preferedRowHeight));
	}

	@Override
	public Dimension getPreferredSize(){
		return new Dimension(this.column0PreferedWidth + this.preferedSettingsWidth, (this.signalPlan.getSignalGroupSettingsDataByGroupId().size() + 1) * this.preferedRowHeight);
	}
	
	

	@Override
	public void valueChanged(ListSelectionEvent e) {
		int[] rows = this.table.getSelectedRows();
		Set<Id> signalGroupIds = new HashSet<Id>();
		for (int i : rows ){
			log.debug("selected row: " + i);
			Id id = (Id) this.table.getValueAt(i, 0);
			signalGroupIds.add(id);
		}
		SignalGroupsSelectionEvent selectionEvent = new SignalGroupsSelectionEvent(true);
		selectionEvent.addSignalGroupIds(this.signalSystemId, signalGroupIds);
		XVisControl.getInstance().getControlEventsManager().fireSelectionEvent(selectionEvent);
	}
}

class SignalPlanTableModel extends AbstractTableModel {

	private SignalPlanData plan;
	private List<SignalGroupSettingsData> settingsList = new ArrayList<SignalGroupSettingsData>(); 

	public SignalPlanTableModel(SignalPlanData signalPlan) {
		this.plan = signalPlan;
		this.settingsList.addAll(this.plan.getSignalGroupSettingsDataByGroupId().values());
	}

	@Override
	public int getRowCount() {
		return this.settingsList.size();
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0){
			return this.settingsList.get(rowIndex).getSignalGroupId();
		}
		else if (columnIndex == 1){
			return this.settingsList.get(rowIndex);
		}
		else {
			throw new RuntimeException("ColumnIndex: " + columnIndex);
		}
	}

	@Override
	public String getColumnName(int column) {
		if (column == 0){
			return "Signal Group Id";
		}
		else if (column == 1){
			return "GreenTime";
		}
		return "";
	}
}

class SignalGroupSettingsRow extends JPanel {

	private static final Logger log = Logger.getLogger(SignalGroupSettingsRow.class);
	
	private SignalGroupSettingsData settings;
	private int cycle;
	private Dimension dimension;
	private float xscale;
	private final int yOffset = 5;
	private final int height;
	
	
	SignalGroupSettingsRow(SignalGroupSettingsData signalGroupSettingsData, int cycle, Dimension d){
		this.settings = signalGroupSettingsData;
		this.cycle = cycle;
		this.dimension = d;
		this.xscale = dimension.width / (float)this.cycle;
		this.height = this.dimension.height - 2*yOffset;
		this.setToolTipText("Onset: " + this.settings.getOnset() + " Dropping: " + this.settings.getDropping());
		this.setMinimumSize(this.dimension);
		this.setMaximumSize(this.dimension);
	}

	@Override
	public Dimension getPreferredSize() {
    return this.dimension;
  }

@Override
public void paintComponent(Graphics g) {
    super.paintComponent(g);       
    g.setColor(SignalPlanPanel.greenColor);
    
    if (settings.getOnset() < settings.getDropping()){
    	int on = (int)(this.xscale *  settings.getOnset());
    	int off = (int)(this.xscale *  (settings.getDropping() - settings.getOnset()));
//    	log.error("xscale: " + this.xscale + " on " + on + " off " + off + " onset: " + settings.getOnset() + " drop: " + settings.getDropping());
    	g.fillRect(on, yOffset, off, this.height);
    }
    else {
    	g.fillRect(0, yOffset, (int)(this.xscale *  settings.getDropping()), this.height);
    	g.fillRect((int)(this.xscale *  settings.getOnset()), yOffset, (int) (this.xscale * this.cycle), this.height);
    }

    //draw the raster
    g.setColor(SignalPlanPanel.textColor);
    int x;
    for (int i = 10; i <= cycle; i = i + 10){
    	x = (int) (i * this.xscale);
    	g.drawLine(x, 0, x, this.dimension.height);
    }
}  

	
}
