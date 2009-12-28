package playground.rost.controller;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.xml.sax.SAXException;

import playground.rost.controller.gui.BlockGUI;
import playground.rost.controller.gui.PopulationDistributionGUI;
import playground.rost.controller.gui.SelectAreaGUI;
import playground.rost.controller.gui.VisualizeFlowGUI;
import playground.rost.controller.gui.VisualizeTimeExpandedPathsGUI;
import playground.rost.controller.gui.helpers.ShowHighwayAttributeSettings;
import playground.rost.controller.gui.helpers.ShowPathTracker;
import playground.rost.controller.gui.helpers.progressinformation.ShowProgressInformationGUI;
import playground.rost.controller.marketplace.FlowMarketPlaceImpl;
import playground.rost.eaflow.ea_flow.Flow;
import playground.rost.eaflow.ea_flow.MultiSourceEAF;
import playground.rost.eaflow.ea_flow.GlobalFlowCalculationSettings.EdgeTypeEnum;
import playground.rost.graph.evacarea.EvacArea;
import playground.rost.graph.nodepopulation.PopulationNodeMap;
import playground.rost.osm2matconverter.OSM2MATConverter;
import playground.rost.util.PathTracker;

public class MainGUI extends JFrame {

	JMenuBar menuBar;
	JMenu mnuFile;
	JMenu mnuOsm2matsim;
	JMenu mnuSelectArea;
	JMenu mnuBlocks;
	JMenu placePpl;
	JMenu mnuFlow;
	JMenu mnuVis;
	
	FlowMarketPlaceImpl fMarket = new FlowMarketPlaceImpl();
	
	JDesktopPane desktop;

	public MainGUI()
	{
		super("Main GUI!");
		JFrame.setDefaultLookAndFeelDecorated(true);

				
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		    setBounds(50, 50,
		              screenSize.width  - 100,
		              screenSize.height - 100);
		
		    //Set up the GUI.
		desktop = new JDesktopPane(); //a specialized layered pane
		setContentPane(desktop);
		
		this.setSize(1000,600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setJMenuBar(createMenuBar());
        setVisible(true);
	}
	
	protected JMenuBar createMenuBar()
	{
		menuBar = new JMenuBar();
	
		createFileMenu();
		createOsm2MatMenu();
		createSelectAreaMenu();
		createBlocksMenu();
		createPlacePplMenu();
		createFlowMenu();
		createVisMenu();
		
		return menuBar;
	}
	
	protected void createFileMenu()
	{
		mnuFile = new JMenu("File");
		
		JMenuItem showPaths = new JMenuItem("Show Paths");
		showPaths.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JInternalFrame newFrame = new ShowPathTracker();
				newFrame.setVisible(true);
				desktop.add(newFrame);
			    try {
		            newFrame.setSelected(true);
		        } catch (java.beans.PropertyVetoException exception){}
			}
		});
		mnuFile.add(showPaths);
		menuBar.add(mnuFile);
	}
	
	protected void createOsm2MatMenu()
	{
		mnuOsm2matsim = new JMenu("OSM Data");
		
		JMenuItem showAttributes = new JMenuItem("Show Highway Attributes");
		showAttributes.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JInternalFrame newFrame = new ShowHighwayAttributeSettings();
				newFrame.setVisible(true);
				desktop.add(newFrame);
			    try {
		            newFrame.setSelected(true);
		        } catch (java.beans.PropertyVetoException exception){}
			}
		});
		mnuOsm2matsim.add(showAttributes);
		
		JMenuItem parseData = new JMenuItem("Parse OSM-Map");
		parseData.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				OSM2MATConverter.parseAndWrite();
			}
		});
		mnuOsm2matsim.add(parseData);
		menuBar.add(mnuOsm2matsim);	
	}
	
	
	protected void createBlocksMenu()
	{
		mnuBlocks = new JMenu("Blocks");
		
		JMenuItem showBlocks = new JMenuItem("Create and Show Blocks");
		showBlocks.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JInternalFrame newFrame = BlockGUI.createBlocksAndShowGUI();
				newFrame.setVisible(true);
				desktop.add(newFrame);
			    try {
		            newFrame.setSelected(true);
		        } catch (java.beans.PropertyVetoException exception){}
			}
		});
		mnuBlocks.add(showBlocks);
		
		menuBar.add(mnuBlocks);
		
	}
	
	protected void createFlowMenu()
	{
		mnuFlow = new JMenu("Flow");
		
		JMenuItem calcEarliestArrivalFlowWithoutBowEdges = new JMenuItem("Calc Earliest Arrival Flow - without BowEdges");
		calcEarliestArrivalFlowWithoutBowEdges.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				calcEAFlowWithoutBowEdges();
			}
		});
		mnuFlow.add(calcEarliestArrivalFlowWithoutBowEdges);

		JMenuItem calcEarliestArrivalFlowWithBowEdges = new JMenuItem("Calc Earliest Arrival Flow - with BowEdges");
		calcEarliestArrivalFlowWithBowEdges.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				calcEAFlowWithBowEdges();
			}
		});
		mnuFlow.add(calcEarliestArrivalFlowWithBowEdges);
		
		mnuFlow.addSeparator();

		menuBar.add(mnuFlow);
	}
	
	protected void calcEAFlowWithBowEdges()
	{
		startCalcEAFlow(EdgeTypeEnum.BOWEDGES_ADD);
	}
	
	protected void startCalcEAFlow(EdgeTypeEnum edgeType)
	{
		EvacArea evacArea = null;
		try {
			evacArea = EvacArea.readXMLFile(PathTracker.resolve("evacArea"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		NetworkLayer network = new NetworkLayer();
		NetworkReaderMatsimV1 nReader = new NetworkReaderMatsimV1(network);
		try {
			nReader.parse(PathTracker.resolve("matExtract"));
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		PopulationNodeMap populationNodeMap = null;
		try {
			populationNodeMap = PopulationNodeMap.readXMLFile(PathTracker.resolve("populationForNodes"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//parse Network
		CalcEAFlow ttCEAF = new CalcEAFlow(evacArea, network, populationNodeMap, edgeType);
		ShowProgressInformationGUI progInfo = new ShowProgressInformationGUI(this, ttCEAF.getMsEAF());
		progInfo.setVisible(true);
		desktop.add(progInfo);
	    try {
            progInfo.setSelected(true);
        } catch (java.beans.PropertyVetoException exception){}
		Thread t = new Thread(ttCEAF);
		t.start();
	}
	
	protected void calcEAFlowWithoutBowEdges()
	{
		startCalcEAFlow(EdgeTypeEnum.SIMPLE);	
	}
	
	protected void createSelectAreaMenu()
	{
		mnuSelectArea = new JMenu("Evacuation Area");
		
		JMenuItem selectArea = new JMenuItem("Select Area");
		selectArea.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JInternalFrame newFrame = SelectAreaGUI.readNetworkAndShowGUI();
				newFrame.setVisible(true);
				desktop.add(newFrame);
			    try {
		            newFrame.setSelected(true);
		        } catch (java.beans.PropertyVetoException exception){}

				
			}
		});
		mnuSelectArea.add(selectArea);
		
		menuBar.add(mnuSelectArea);
	}
	
	protected void createPlacePplMenu()
	{
		placePpl = new JMenu("Population Distribution");
		
		JMenuItem defineDistribution = new JMenuItem("Define Distribution");
		defineDistribution.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JInternalFrame newFrame = PopulationDistributionGUI.parseNetworkAndBlocksAndShowGUI();
				newFrame.setVisible(true);
				desktop.add(newFrame);
			    try {
		            newFrame.setSelected(true);
		        } catch (java.beans.PropertyVetoException exception){}

			}
		});
		placePpl.add(defineDistribution);
		
		menuBar.add(placePpl);
		
		
	}
	
	protected void createVisMenu()
	{
		mnuVis = new JMenu("Visualization");
		
		JMenuItem flowVis = new JMenuItem("Visualize Flow");
		flowVis.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String id = selectFlow();
				if(id  != null && id.length() > 0)
				{
					Flow flow = fMarket.getElement(id);
					JInternalFrame newFrame = new VisualizeFlowGUI(flow.getNetwork(),flow);
			        addFrame(newFrame);
				}
			}
		});
		mnuVis.add(flowVis);
		
		JMenuItem pathVis = new JMenuItem("Visualize Paths");
		pathVis.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String id = selectFlow();
				if(id  != null && id.length() > 0)
				{
					Flow flow = fMarket.getElement(id);
					JInternalFrame newFrame = new VisualizeTimeExpandedPathsGUI(flow.getNetwork(),flow);
			        addFrame(newFrame);
				}
			}
		});
		mnuVis.add(pathVis);
		
		menuBar.add(mnuVis);
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new MainGUI();
	}
	
	protected class CalcEAFlow implements Runnable
	{

		public MultiSourceEAF getMsEAF() {
			return msEAF;
		}

		public boolean isFinished() {
			return isFinished;
		}

		public Flow getResult() {
			return result;
		}

		protected MultiSourceEAF msEAF;
		protected boolean isFinished;
		protected Flow result = null;

		protected EvacArea evacArea;
		protected NetworkLayer network;
		protected PopulationNodeMap populationNodeMap;
		
		public CalcEAFlow(EvacArea evacArea, NetworkLayer network, PopulationNodeMap populationNodeMap, EdgeTypeEnum edgeType)
		{
			this.evacArea = evacArea;
			this.network = network;
			this.populationNodeMap = populationNodeMap;
			this.isFinished = false;
			this.msEAF = new MultiSourceEAF();
			this.msEAF.setEdgeTypeToUse(edgeType);
		}
		
		public void run() {
			result = msEAF.calcEAFlow(evacArea, network, populationNodeMap);
			isFinished = true;
			
			String id = fMarket.addElement(result);
			JOptionPane.showMessageDialog(getMainFrame(),
				     id + " added!",
				    "Calculation Completed",
				    JOptionPane.INFORMATION_MESSAGE);
			
			createMenuItemForFlow(id);
		}
	}
	
	protected void addFrame(JInternalFrame newFrame)
	{
		newFrame.setVisible(true);
		desktop.add(newFrame);
	    try {
            newFrame.setSelected(true);
        } catch (java.beans.PropertyVetoException exception){}
	}
	
	protected JFrame getMainFrame()
	{
		return this;
	}
	
	protected void createMenuItemForFlow(String id)
	{
		JMenuItem removeFlow = new JMenuItem("remove: " + id);
		removeFlow.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(e.getSource() != null)
				{
					if(e.getSource() instanceof JMenuItem)
					{
						JMenuItem item = (JMenuItem)e.getSource();
						String text = item.getText();
						int i = text.indexOf(": ", 0);
						String id  = text.substring(i+2);
						removeFlow(id);
						mnuFlow.remove(item);
					}
				}
			}
		});
		mnuFlow.add(removeFlow);
	}
	
	protected void removeFlow(String id)
	{
		if(!fMarket.removeElement(id))
		{
			JOptionPane.showMessageDialog(this, "Remove Flow", "Flow could not be find!", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	protected String selectFlow()
	{
		Collection<String> ids = fMarket.getIds();
		if(ids.size() > 0)
		{
			String[] idsArray = new String[ids.size()];
			int i = 0;
			for(String id : ids)
			{
				idsArray[i++] = id;
			}
			String s = (String)JOptionPane.showInputDialog(
			                    this,
			                    "Complete the sentence:\n"
			                    + "\"Green eggs and...\"",
			                    "Customized Dialog",
			                    JOptionPane.PLAIN_MESSAGE,
			                    null,
			                    idsArray,
			                    idsArray[0]);
			if(s != null && s.length() > 0)
			{
				return s;
			}
		}
		return null;

	}
	
}
