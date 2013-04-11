package playground.wdoering.grips.scenariomanager.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import playground.wdoering.grips.scenariomanager.control.AbstractListener;
import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;

public class DefaultWindow extends JFrame
{
	protected Controller controller;
	protected final Visualizer visualizer;
	
	private int width;
	private int height;
	private int border;
	private BufferedImage image;
	protected JPanel mainPanel;
	private AbstractToolBox toolBox;

	public DefaultWindow(Controller controller)
	{
		if (controller.isMainWindowUndecorated())
			this.setUndecorated(true);

		this.controller = controller;
		this.visualizer = controller.getVisualizer();
		
		this.width = this.controller.getImageContainer().getWidth();
		this.height = this.controller.getImageContainer().getHeight();
		this.border = this.controller.getImageContainer().getBorderWidth();
		this.image = this.controller.getImageContainer().getImage();
		
		this.setLayout(new BorderLayout(this.border/2,this.border/2));
		
		this.mainPanel = new DefaultRenderPanel(this.visualizer, this.image, this.border);
		this.mainPanel.setPreferredSize(new Dimension(this.width, this.height));
		this.mainPanel.setSize(new Dimension(this.width, this.height));
		
		if (this.controller.getActiveToolBox()!=null)
		{
			this.toolBox = this.controller.getActiveToolBox();
//			this.toolBox.setBackground(new Color(0,0,255));
			this.add(this.toolBox, BorderLayout.EAST);
		}
//		else
//		{
//			JPanel test = new JPanel();
//			test.setBackground(Color.GREEN);
//			this.add(test, BorderLayout.EAST);
//		}
			
		
//		this.add(new JLabel(" "), BorderLayout.NORTH);
//		this.add(new JLabel(" "), BorderLayout.SOUTH);
		mainPanel.setBackground(Color.blue);
		this.add(mainPanel, BorderLayout.CENTER);
		
		int margin = Math.max(1,(this.getComponentCount()-1)) * this.border + 3*this.border;
		
		this.setPreferredSize(new Dimension(this.width + margin, this.height + margin));
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.pack();
		this.validate();
		this.setLocationRelativeTo(null);
		this.setVisible(true);

		
	}
	
	public void setToolBox(AbstractToolBox toolBox)
	{
		if (this.toolBox!=null)
			this.remove(this.toolBox);
		
		this.toolBox = toolBox;
		this.add(this.toolBox, BorderLayout.EAST);
		this.toolBox.setVisible(true);
		this.validate();
	}
	
	public AbstractToolBox getToolBox()
	{
		return toolBox;
	}
	
	public JPanel getMainPanel()
	{
		return mainPanel;
	}
	
	public void setMainPanel(JPanel mainPanel)
	{
		if (this.mainPanel != null)
			this.remove(this.mainPanel);
		
		this.mainPanel = mainPanel;
		this.add(mainPanel, BorderLayout.CENTER);
		this.validate();
	}
	
	public void updateMask()
	{
		
	}

	public void setToolBoxVisible(boolean toggle)
	{
		if (this.toolBox !=null)
		this.toolBox.setVisible(toggle);
	}



}
