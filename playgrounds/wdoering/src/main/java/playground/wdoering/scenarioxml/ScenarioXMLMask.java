/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wdoering.scenarioxml;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.matsim.contrib.grips.control.Controller;
import org.matsim.contrib.grips.control.ShapeFactory;
import org.matsim.contrib.grips.io.GripsConfigDeserializer;
import org.matsim.contrib.grips.io.GripsConfigSerializer;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.DepartureTimeDistributionType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.DistributionType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.MainTrafficTypeType;
import org.matsim.contrib.grips.model.AbstractModule;
import org.matsim.contrib.grips.model.AbstractToolBox;
import org.matsim.contrib.grips.model.Constants;
import org.matsim.contrib.grips.model.Constants.ModuleType;
import org.matsim.contrib.grips.model.SelectionModeSwitch;
import org.matsim.contrib.grips.model.config.GripsConfigModule;
import org.matsim.contrib.grips.model.shape.PolygonShape;
import org.matsim.contrib.grips.model.shape.Shape;
import org.matsim.contrib.grips.scenariogenerator.SGMask;
import org.matsim.contrib.grips.scenariogenerator.ScenarioGenerator;
import org.matsim.contrib.grips.view.DefaultOpenDialog;
import org.matsim.contrib.grips.view.DefaultSaveDialog;
import org.matsim.core.config.Config;

/**
 * @author wdoering
 * 
 */
class ScenarioXMLMask extends JPanel implements ActionListener {
	
	
	private static final long serialVersionUID = 1L;
	// Fields shall be moved to ScenarioXMLToolBox later
	
	//elements are defined in the order they appear in the mask
	
	
	private JLabel labelCurrentFile;
	
	//osm file
	private JLabel labelOSMFilePath;
	private JButton btOSMBrowse;
	
	//main traffic type
	private JComboBox boxTrafficType;
	
	//evac file
	private JLabel labelEvacFilePath;
	private JButton btEvacBrowse;
	
	//population file
	private JLabel labelPopFilePath;
	private JButton btPopBrowse;
	
	//output directory
	private JLabel labelOutDirPath;
	private JButton btOutDirBrowse;
	
	//sample size
	private JTextField textFieldSampleSize;
	public JLabel labelSampleSize;
	private JSlider sliderSampleSize;
	
	//dep time
	private JComboBox boxDepTime;
	
	//sigma
	private JLabel labelSigma;
	private JTextField textFieldSigma;
	
	//mu
	private JLabel labelMu;
	private JTextField textFieldMu;
	
	//earliest
	private JLabel labelEarliest;
	private JTextField textFieldEarliest;
	
	//latest
	private JLabel labelLatest;
	private JTextField textFieldLatest;
	
	private Controller controller;

	private JButton btNew;
	private JButton btOpen;
	private JButton btSave;
	private GripsConfigModule gcm;
	private boolean configOpened;
	private String fileLocation;
	private String[] trafficTypeStrings;

	private String[] distTypeStrings;

	private void initComponents() {

		
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        Border border = BorderFactory.createLineBorder(Color.BLACK, 1);
        Border emptyBorder = BorderFactory.createEmptyBorder(15,15,15,15);
        
        Dimension inputSize = new Dimension(400,30);
        Dimension varInputSize = new Dimension(100,20);
        
        
        
        
        JPanel panelCurrentFile = new JPanel();
        labelCurrentFile = new JLabel(" / ");
        labelCurrentFile.setForeground(Color.GRAY);
        panelCurrentFile.setBorder(BorderFactory.createTitledBorder(border, controller.getLocale().labelCurrentFile()));
        panelCurrentFile.add(labelCurrentFile);
        
        
        JPanel panelOSM = new JPanel();
        labelOSMFilePath = new JLabel(" / ");
        labelOSMFilePath.setForeground(Color.GRAY);
        labelOSMFilePath.setPreferredSize(inputSize);
        btOSMBrowse = new JButton(controller.getLocale().btSet());
        btOSMBrowse.addActionListener(this);
        btOSMBrowse.setActionCommand(controller.getLocale().labelNetworkFile());
        panelOSM.setBorder(BorderFactory.createTitledBorder(border, controller.getLocale().labelNetworkFile()));
        panelOSM.add(labelOSMFilePath);
        panelOSM.add(btOSMBrowse);

        //prepare main traffic type elements
        MainTrafficTypeType[] trafficTypeElements = MainTrafficTypeType.values();
        trafficTypeStrings = new String[trafficTypeElements.length];
        for (int i = 0; i < trafficTypeElements.length; i++)
        	trafficTypeStrings[i] = trafficTypeElements[i].toString();
//      TODO: englishElements convert english to native language elements
        
        //main traffic type
        JPanel panelTrafficType = new JPanel();
        boxTrafficType = new JComboBox(trafficTypeStrings);
        boxTrafficType.setPreferredSize(inputSize);
        boxTrafficType.setActionCommand(controller.getLocale().labelTrafficType());
        boxTrafficType.addActionListener(this);

        panelTrafficType.setBorder(BorderFactory.createTitledBorder(border, controller.getLocale().labelTrafficType()));
        panelTrafficType.add(boxTrafficType);
        
        JPanel panelEvac = new JPanel();
        labelEvacFilePath = new JLabel(" / ");
        labelEvacFilePath.setForeground(Color.GRAY);
        labelEvacFilePath.setPreferredSize(inputSize);
        btEvacBrowse = new JButton(controller.getLocale().btSet());
        btEvacBrowse.addActionListener(this);
        btEvacBrowse.setActionCommand(controller.getLocale().labelEvacFile());

        panelEvac.setBorder(BorderFactory.createTitledBorder(border, controller.getLocale().labelEvacFile()));
        panelEvac.add(labelEvacFilePath);
        panelEvac.add(btEvacBrowse);
        
        JPanel panelPop = new JPanel();
        labelPopFilePath = new JLabel(" / ");
        labelPopFilePath.setForeground(Color.GRAY);
        labelPopFilePath.setPreferredSize(inputSize);
        btPopBrowse = new JButton(controller.getLocale().btSet());
        btPopBrowse.addActionListener(this);
        btPopBrowse.setActionCommand(controller.getLocale().labelPopFile());
        panelPop.setBorder(BorderFactory.createTitledBorder(border, controller.getLocale().labelPopFile()));
        panelPop.add(labelPopFilePath);
        panelPop.add(btPopBrowse);
        
        JPanel panelOutDir = new JPanel();
        labelOutDirPath = new JLabel(" / ");
        labelOutDirPath.setPreferredSize(inputSize);
        labelOutDirPath.setForeground(Color.GRAY);
        btOutDirBrowse = new JButton(controller.getLocale().btSet());
        btOutDirBrowse.addActionListener(this);
        btOutDirBrowse.setActionCommand(controller.getLocale().labelOutDir());

        panelOutDir.setBorder(BorderFactory.createTitledBorder(border, controller.getLocale().labelOutDir()));
        panelOutDir.add(labelOutDirPath);
        panelOutDir.add(btOutDirBrowse);
        
        JPanel panelSampleSize = new JPanel();
        labelSampleSize = new JLabel("0.787");
        labelSampleSize.setPreferredSize(varInputSize);
        sliderSampleSize = new JSlider(1, 1000, 787);
        sliderSampleSize.setOrientation(JSlider.HORIZONTAL);
        sliderSampleSize.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				labelSampleSize.setText(""+(sliderSampleSize.getValue()/1000d));
				
				checkSaveConditions();

				
			}
		});
        panelSampleSize.setBorder(BorderFactory.createTitledBorder(border, controller.getLocale().labelSampleSize()));
        panelSampleSize.add(labelSampleSize);
        panelSampleSize.add(sliderSampleSize);
        
        //prepare main traffic type elements
        DistributionType[] distTypeElements = DistributionType.values();
        distTypeStrings = new String[distTypeElements.length];
        for (int i = 0; i < distTypeElements.length; i++)
        	distTypeStrings[i] = distTypeElements[i].toString();
//      TODO: englishElements convert english to native language elements
        
        JPanel panelDepTime = new JPanel();
        panelDepTime.setLayout(new BoxLayout(panelDepTime, BoxLayout.PAGE_AXIS));
        boxDepTime = new JComboBox(distTypeStrings);
        boxDepTime.setSelectedIndex(1);
        boxDepTime.setBorder(emptyBorder);
        boxDepTime.setActionCommand(controller.getLocale().labelDepTime());
        boxDepTime.addActionListener(this);
//        boxDepTime.setPreferredSize(inputSize);
//        boxDepTime.setMinimumSize(inputSize);
        
        JPanel panelParams = new JPanel();
        panelParams.setLayout(new GridLayout(2, 4));
        labelSigma = new JLabel(" " + controller.getLocale().labelSigma());
        textFieldSigma = new JTextField("0.25");
        textFieldSigma.setPreferredSize(varInputSize);
//        textFieldSigma.setBorder(emptyBorder);
        
        labelMu = new JLabel(" " + controller.getLocale().labelMu());
        textFieldMu = new JTextField("0.1");
        textFieldMu.setPreferredSize(varInputSize);
//        textFieldMu.setBorder(emptyBorder);
        
        labelEarliest = new JLabel(" " + controller.getLocale().labelEarliest());
        textFieldEarliest = new JTextField("0.04315872");
        textFieldEarliest.setPreferredSize(varInputSize);
//        textFieldEarliest.setBorder(emptyBorder);
        
        labelLatest = new JLabel(" " + controller.getLocale().labelLatest());
        textFieldLatest = new JTextField("1.3783154");
        textFieldLatest.setPreferredSize(varInputSize);
//        textFieldLatest.setBorder(emptyBorder);
        
        JPanel panelIO = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelIO.setBackground(new Color(190,190,190));
        btNew = new JButton(controller.getLocale().btNew());
        btOpen = new JButton(controller.getLocale().btOpen());
        btSave = new JButton(controller.getLocale().btSave());
        btNew.addActionListener(this);
        btOpen.addActionListener(this);
        btSave.addActionListener(this);
        btSave.setEnabled(false);
        
        panelIO.add(btNew);
        panelIO.add(btOpen);
        panelIO.add(btSave);
        
        panelParams.add(labelSigma);
        panelParams.add(textFieldSigma);
        panelParams.add(labelMu);
        panelParams.add(textFieldMu);
        panelParams.add(labelEarliest);
        panelParams.add(textFieldEarliest);
        panelParams.add(labelLatest);
        panelParams.add(textFieldLatest);
        
        panelDepTime.setBorder(BorderFactory.createTitledBorder(border, controller.getLocale().labelDepTime()));
        panelDepTime.add(boxDepTime);
        panelDepTime.add(panelParams);
        
        this.add(panelCurrentFile);
        this.add(panelOSM);
        this.add(panelTrafficType);
        this.add(panelEvac);
        this.add(panelPop);
        this.add(panelOutDir);
        this.add(panelSampleSize);
        this.add(panelDepTime);
        this.add(panelIO);
        
	}

	ScenarioXMLMask(AbstractModule module, Controller controller) {
		this.controller = controller;
		this.setLayout(new BorderLayout());
		initComponents();
	}
	
	public void readConfig()
	{
		GripsConfigModule gcm = this.controller.getGripsConfigModule();
//		String nfn = gcm.getNetworkFileName();
//		this.textFieldNetworkFile.setText(nfn);
//		String mtt = gcm.getMainTrafficType();
//		this.textFieldTrafficType.setText(mtt);
		
//		this.btRun.setEnabled(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getActionCommand().equals(controller.getLocale().labelNetworkFile()))
		{
			DefaultOpenDialog openDialog = new DefaultOpenDialog(controller, ".osm", "network file", false);
			openDialog.showDialog(controller.getParentComponent(), null);
			if (openDialog.getSelectedFile()!=null)
			{
				this.labelOSMFilePath.setText(openDialog.getSelectedFile().getAbsolutePath());
			}
		}
		else if (e.getActionCommand().equals(controller.getLocale().labelEvacFile()))
		{
			DefaultSaveDialog saveDialog = new DefaultSaveDialog(controller, ".shp", "evacuation file", false);
			saveDialog.showDialog(controller.getParentComponent(), null);
			if (saveDialog.getSelectedFile()!=null)
			{
				if (saveDialog.getSelectedFile().getAbsolutePath().equals(labelPopFilePath.getText()))
					JOptionPane.showMessageDialog(this, controller.getLocale().msgSameFiles(),"",JOptionPane.ERROR_MESSAGE);
				else
					this.labelEvacFilePath.setText(saveDialog.getSelectedFile().getAbsolutePath());
			}
			
		}
		else if (e.getActionCommand().equals(controller.getLocale().labelPopFile()))
		{
			
			DefaultSaveDialog saveDialog = new DefaultSaveDialog(controller, ".shp", "population file", false);
			saveDialog.showDialog(controller.getParentComponent(), null);
			if (saveDialog.getSelectedFile()!=null)
			{
				if (saveDialog.getSelectedFile().getAbsolutePath().equals(labelEvacFilePath.getText()))
					JOptionPane.showMessageDialog(this, controller.getLocale().msgSameFiles(),"",JOptionPane.ERROR_MESSAGE);
				else
					this.labelPopFilePath.setText(saveDialog.getSelectedFile().getAbsolutePath());
			}
		}
		else if (e.getActionCommand().equals(controller.getLocale().labelOutDir()))
		{
			DefaultOpenDialog openDialog = new DefaultOpenDialog(controller, "", "directory", true);
			openDialog.showDialog(controller.getParentComponent(), "select output directory");
			if (openDialog.getSelectedFile()!=null)
			{
				this.labelOutDirPath.setText(openDialog.getSelectedFile().getAbsolutePath());
				this.gcm = null;
				this.configOpened = true;
			}
			
			
			
		}
		else if (e.getActionCommand().equals(controller.getLocale().btNew()))
		{
			DefaultSaveDialog save = new DefaultSaveDialog(controller, ".xml", "GRIPS config file", true);
			save.showDialog(this.controller.getParentComponent(), "Save GRIPS config file");
			if (save.getSelectedFile()!=null)
			{
				this.labelCurrentFile.setText(save.getSelectedFile().getAbsolutePath());
				this.labelEvacFilePath.setText(" / ");
				this.labelPopFilePath.setText(" / ");
				this.labelOSMFilePath.setText(" / ");
				this.labelOutDirPath.setText(save.getSelectedFile().getParent() + "/");
				this.fileLocation = save.getSelectedFile().getAbsolutePath();
				this.configOpened = true;
				
				controller.setGoalAchieved(false);

			}
			
		}
		else if (e.getActionCommand().equals(controller.getLocale().btOpen()))
		{
			this.setEnabled(false);
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			setMaskEnabled(false);

			SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

				@Override
				protected String doInBackground() {
					if (controller.openGripsConfig())
					{
						
						fileLocation = controller.getGripsFile();
						gcm = controller.getGripsConfigModule();
						configOpened = true;
						setMaskEnabled(true);
						controller.setGoalAchieved(true);
						btSave.setEnabled(false);
						
						//enable other modules if shape files exist
						if (!controller.isStandAlone())
						{
							File evacFile = new File(gcm.getEvacuationAreaFileName());
							if(evacFile.exists())
								controller.enableModule(ModuleType.POPULATION);
							File popFile = new File(gcm.getEvacuationAreaFileName());
							if(popFile.exists())
							{
								controller.enableModule(ModuleType.GRIPSSCENARIO);
								controller.setPopulationFileOpened(true);
							}
							
							controller.updateParentUI();
						}
						
					}
					return "";
				}

				@Override
				protected void done() {
					setEnabled(true);
					setCursor(Cursor.getDefaultCursor());
					updateMask();
					
					

				}
			};

			worker.execute();
			
			
		}
		else if (e.getActionCommand().equals(controller.getLocale().btSave()))
		{
			if (configOpened)
			{
				if (this.gcm == null)
					this.gcm = new GripsConfigModule(fileLocation);
				
				this.gcm.setOutputDir(this.labelOutDirPath.getText());
				this.gcm.setPopulationFileName(this.labelPopFilePath.getText());
				this.gcm.setEvacuationAreaFileName(this.labelEvacFilePath.getText());
				this.gcm.setNetworkFileName(this.labelOSMFilePath.getText());
				
				this.gcm.setMainTrafficType(boxTrafficType.getSelectedItem().toString().toLowerCase());
				DepartureTimeDistributionType dtdt = new DepartureTimeDistributionType();
				dtdt.setDistribution(DistributionType.valueOf(boxDepTime.getSelectedItem().toString().toUpperCase().replaceAll("-", "_")));
				dtdt.setSigma(Double.valueOf(textFieldSigma.getText()));
				dtdt.setMu(Double.valueOf(textFieldMu.getText()));
				dtdt.setEarliest(Double.valueOf(textFieldEarliest.getText()));
				dtdt.setLatest(Double.valueOf(textFieldLatest.getText()));
				this.gcm.setDepartureTimeDistribution(dtdt);
				this.gcm.setSampleSize(labelSampleSize.getText());
				
				boolean writeConfig = controller.writeGripsConfig(this.gcm,this.fileLocation);
				if (writeConfig)
				{
					this.controller.setGoalAchieved(true);
					this.btSave.setEnabled(false);
				}
			}
			
			
		}
		
		checkSaveConditions();
		
	}

	private void checkSaveConditions() {
		if ((this.configOpened) &&
			(!this.labelCurrentFile.getText().equals(" / ")) &&
			(!this.labelEvacFilePath.getText().equals(" / ")) &&
			(!this.labelPopFilePath.getText().equals(" / ")) &&
			(!this.labelOutDirPath.getText().equals(" / ")))
			this.btSave.setEnabled(true);
		else
			this.btSave.setEnabled(false);
	}
	
	public void updateMask() {
		this.labelCurrentFile.setText(this.fileLocation);
		this.labelOSMFilePath.setText(gcm.getNetworkFileName());
		this.labelEvacFilePath.setText(gcm.getEvacuationAreaFileName());
		this.labelPopFilePath.setText(gcm.getPopulationFileName());
		this.labelOutDirPath.setText(gcm.getOutputDir());
		
		String gcmMTT = gcm.getMainTrafficType().toLowerCase();
//		System.out.println(gcmMTT + " | " + trafficTypeStrings[0]);
		for (int i = 0; i < trafficTypeStrings.length; i++)
		{
			if (trafficTypeStrings[i].toLowerCase().equals(gcmMTT))
				this.boxTrafficType.setSelectedIndex(i);
		}
		
		DepartureTimeDistributionType gcmDep = gcm.getDepartureTimeDistribution();
		String gcmDepType = gcmDep.getDistribution().toString().toLowerCase();
//		System.out.println(gcmDepType + " | " + distTypeStrings[0]);
		for (int i = 0; i < distTypeStrings.length; i++)
		{
			if (distTypeStrings[i].toLowerCase().equals(gcmDepType))
				this.boxDepTime.setSelectedIndex(i);
		}
		
		this.textFieldSigma.setText(gcmDep.getSigma()+"");
		this.textFieldMu.setText(gcmDep.getMu()+"");
		this.textFieldEarliest.setText(gcmDep.getEarliest()+"");
		this.textFieldLatest.setText(gcmDep.getLatest()+"");
		
		this.sliderSampleSize.setValue((int)(gcm.getSampleSize()*1000));
		
		
		
		
	}

	public void setMaskEnabled(boolean b)
	{
		this.btEvacBrowse.setEnabled(b);
		this.btPopBrowse.setEnabled(b);
		this.btOutDirBrowse.setEnabled(b);
		this.btOSMBrowse.setEnabled(b);
		this.sliderSampleSize.setEnabled(b);
		this.textFieldEarliest.setEnabled(b);
		this.textFieldLatest.setEnabled(b);
		this.textFieldMu.setEnabled(b);
		this.textFieldSigma.setEnabled(b);
		
		this.boxDepTime.setEnabled(b);
		this.boxTrafficType.setEnabled(b);
		this.btOpen.setEnabled(b);
		this.btNew.setEnabled(b);
		this.btSave.setEnabled(b);
		
		
	}


}