package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class LinkAddedDialog extends JPanel
{
	private JOptionPane optionPane;
	
	
	static String[] modes = {"car", "pt", "car, pt"};
	
	protected static JCheckBox drawnDirection=new JCheckBox("Drawn Direction");
	protected static JCheckBox reverseDirection=new JCheckBox("Reverse Direction");
	
	protected static JTextField freeSpeed =new JTextField();
	protected static JTextField capacity =new JTextField();
	protected static JTextField numberOfLanes =new JTextField();
	protected static JComboBox allowedModes = new JComboBox(modes);
	protected static JTextField length=new JTextField();
	
	protected static JTextField freeSpeedRev =new JTextField();
	protected static JTextField capacityRev =new JTextField();
	protected static JTextField numberOfLanesRev =new JTextField();
	protected static JComboBox allowedModesRev = new JComboBox(modes);
	protected static JTextField lengthRev=new JTextField();

	public LinkAddedDialog(String linkLength)
	{
		
		//--------------layout---------------------------------------------
		
		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());
		
		//--------------displayed elements---------------------------------
		
		final JLabel newLink= new JLabel(tr("New Link"));
		final JLabel freeSpeedLabel = new JLabel(tr("Freespeed"));
		final JLabel capacityLabel = new JLabel(tr("Capacity"));
		final JLabel lanesLabel= new JLabel(tr("Number of Lanes"));
		final JLabel allowedModesLabel = new JLabel(tr("Allowed Modes"));
		final JLabel lengthLabel = new JLabel(tr("Length"));
		
		//--------------arrangement of elements-----------------------------
		
		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = 4;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		add(newLink, c);

		c.gridx=1;
		c.gridy=1;
		c.gridwidth = 1;
		add(drawnDirection,c);
		c.gridx=2;
		add(reverseDirection,c);
		
		c.gridx = 0;
		c.gridy = 2;
		add(freeSpeedLabel, c);
		c.gridx = 1;
		add(freeSpeed, c);
		c.gridx = 2;
		add(freeSpeedRev, c);
		
		c.gridx = 0;
		c.gridy = 3;
		add(capacityLabel, c);
		c.gridx = 1;
		add(capacity, c);
		c.gridx = 2;
		add(capacityRev, c);
		
		c.gridx = 0;
		c.gridy = 4;
		add(lanesLabel, c);
		c.gridx = 1;
		add(numberOfLanes, c);
		c.gridx = 2;
		add(numberOfLanesRev, c);
		
		c.gridx = 0;
		c.gridy = 5;
		add(allowedModesLabel, c);
		c.gridx=1;
		add(allowedModes, c);
		c.gridx=2;
		add(allowedModesRev, c);
		
		c.gridx=0;
		c.gridy = 6;
		add(lengthLabel, c);
		c.gridx=1;
		add(length, c);
		c.gridx=2;
		add(lengthRev,c);
		
		//--------------configure initial setup-----------------------------
		
		drawnDirection.setSelected(true);
		reverseDirection.setSelected(true);
		
		capacity.setText("0.000");
		capacityRev.setText("0.000");
		freeSpeed.setText("0.000");
		freeSpeedRev.setText("0.000");
		numberOfLanes.setText("0.000");
		numberOfLanesRev.setText("0.000");
		
		freeSpeed.setEnabled(drawnDirection.isSelected());
		capacity.setEnabled(drawnDirection.isSelected());
		numberOfLanes.setEnabled(drawnDirection.isSelected());
		allowedModes.setEnabled(drawnDirection.isSelected());
		freeSpeedRev.setEnabled(reverseDirection.isSelected());
		capacityRev.setEnabled(reverseDirection.isSelected());
		numberOfLanesRev.setEnabled(reverseDirection.isSelected());
		allowedModesRev.setEnabled(reverseDirection.isSelected());
		
		length.setEnabled(false);
		lengthRev.setEnabled(false);
		length.setText(linkLength);
		lengthRev.setText(linkLength);
		
		//--------------action listeners------------------------------------
		
		drawnDirection.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(!(drawnDirection.isSelected()))
				{
					freeSpeed.setEnabled(false);
					capacity.setEnabled(false);
					numberOfLanes.setEnabled(false);
					allowedModes.setEnabled(false);
				}
				else if(drawnDirection.isSelected())
				{
					freeSpeed.setEnabled(true);
					capacity.setEnabled(true);
					numberOfLanes.setEnabled(true);
					allowedModes.setEnabled(true);
				}
				
			}
		});
		
		reverseDirection.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(!(reverseDirection.isSelected()))
				{
					freeSpeedRev.setEnabled(false);
					capacityRev.setEnabled(false);
					numberOfLanesRev.setEnabled(false);
					allowedModesRev.setEnabled(false);
				}
				else if(reverseDirection.isSelected())
				{
					freeSpeedRev.setEnabled(true);
					capacityRev.setEnabled(true);
					numberOfLanesRev.setEnabled(true);
					allowedModesRev.setEnabled(true);
				}
				
			}
		});
		
	}

	public void setOptionPane(JOptionPane optionPane)
	{
		this.optionPane = optionPane;
	}

	
	
}

