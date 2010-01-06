/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQueryControl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryOptions;


/**
 * @author dgrether
 *
 */
public class OTFQueryControlToolBar extends JToolBar implements ActionListener, ItemListener {

  private static final Logger log = Logger.getLogger(OTFQueryControlToolBar.class);
  
  private JTextField textField;
  private JComboBox queryTypeComboBox;
  private JTabbedPane pane;

  private final OTFVisConfig config;

  private final OTFQueryControl queryControl; 


  public OTFQueryControlToolBar(OTFQueryControl queryControl, final OTFVisConfig config) {
    super();
    this.config = config;
    this.queryControl = queryControl;
    initGui();
  }
  
  private void initGui(){
    this.pane = new JTabbedPane();
    this.add(pane);
    JPanel com = new JPanel();
    com.setSize(500, 60);
    {
      JLabel jLabel3 = new JLabel();
      com.add(jLabel3);
      jLabel3.setText("Query:");
      jLabel3.setBounds(344, 45, 36, 31);
    }
    {
      ComboBoxModel leftMFuncModel =  new DefaultComboBoxModel(this.queryControl.getQueries());
      leftMFuncModel.setSelectedItem(this.queryControl.getQueries().get(0));
      queryTypeComboBox = new JComboBox();
      com.add(queryTypeComboBox);
      queryTypeComboBox.setActionCommand("type_changed");
      queryTypeComboBox.setModel(leftMFuncModel);
      queryTypeComboBox.setBounds(57, 76, 92, 27);
      queryTypeComboBox.setMaximumSize(new Dimension(250,60));
      queryTypeComboBox.addActionListener(this);
      queryTypeComboBox.setToolTipText(this.queryControl.getQueries().get(0).toolTip);
    }
    {
      JLabel jLabel3 = new JLabel();
      com.add(jLabel3);
      jLabel3.setText(" Id:");
      textField = new JTextField();
      com.add(textField);
      textField.setActionCommand("id_changed");
      textField.setPreferredSize(new Dimension(350,30));
      textField.setMaximumSize(new Dimension(350,40));
      textField.addActionListener(this);
    }
    {
      JLabel jLabel3 = new JLabel();
      com.add(jLabel3);
      jLabel3.setText("  ");

      JButton button = new JButton();
      button.setText("Clear!");
      button.setActionCommand("clear");
      button.addActionListener(this);
      button.setToolTipText("Clears all queries!");
      com.add(button);

      JCheckBox SynchBox = new JCheckBox("multiple select");
      SynchBox.setMnemonic(KeyEvent.VK_M);
      SynchBox.setSelected(config.isMultipleSelect());
      SynchBox.addItemListener(this);
      com.add(SynchBox);
    }
    com.doLayout();
    com.setBounds(250, 130, 220, 160);
    pane.addTab("Query", com);
  }

  public void actionPerformed(ActionEvent e) {
    OTFVisConfig cfg = this.config;
    String command = e.getActionCommand();
    if("id_changed".equals(command)) {
      String id = ((JTextField)e.getSource()).getText();
      if (!cfg.isMultipleSelect()){
        this.queryControl.removeQueries();
      }
      this.queryControl.handleIdQuery(id, cfg.getQueryType());
    } 
    else if ("type_changed".equals(command)) {
      JComboBox cb = (JComboBox)e.getSource();
      QueryEntry queryType = (QueryEntry)cb.getSelectedItem();
      cfg.setQueryType(queryType.clazz.getCanonicalName());
      cfg.setQueryType(queryType.clazz.getCanonicalName());
      this.queryControl.removeQueries();
      OTFQuery test = this.queryControl.createQuery(queryType.clazz.getCanonicalName());

      if(pane.getTabCount() > 1){
        pane.remove(1);
      }

      if(test instanceof OTFQueryOptions) {
        JComponent settings = ((OTFQueryOptions)test).getOptionsGUI(pane);
        pane.addTab("Options", settings);
      }

      cb.setToolTipText(queryType.toolTip);
    } else if ("clear".equals(command)) {
      this.queryControl.removeQueries();
    }
  }

  public void itemStateChanged(ItemEvent e) {
    JCheckBox source = (JCheckBox)e.getItemSelectable();
    if (source.getText().equals("multiple select")) {
      this.config.setMultipleSelect(e.getStateChange() != ItemEvent.DESELECTED);
    }
  }

  
  public JTextField getTextField() {
    return textField;
  } 
}
