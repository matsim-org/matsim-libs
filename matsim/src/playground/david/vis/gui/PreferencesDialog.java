package playground.david.vis.gui;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;




/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class PreferencesDialog extends javax.swing.JDialog implements ChangeListener, ActionListener {

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel("apple.laf.AquaLookAndFeel");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private final OTFVisConfig cfg;
	private JLabel jLabel2;
	private JLabel jLabel4;
	private JComboBox rightMFunc;
	private JComboBox middleMFunc;
	private JComboBox leftMFunc;
	private JLabel jLabel3;
	private JLabel jLabel1;
	private OTFHostControlBar host = null;

	/**
	* Auto-generated main method to display this JDialog
	*/
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new JFrame();
				PreferencesDialog inst = new PreferencesDialog(frame, new OTFVisConfig(), null);
				inst.setVisible(true);
			}
		});
	}
	
	public PreferencesDialog(JFrame frame, OTFVisConfig config, OTFHostControlBar mother) {
		super(frame);
		cfg = config;
		this.host = mother;
		initGUI();
	}
	
	private void initGUI() {
		try {
			getContentPane().setLayout(null);
			this.setResizable(false);
			{
				jLabel1 = new JLabel();
				getContentPane().add(jLabel1);
				jLabel1.setText("Left");
				jLabel1.setBounds(117, 45, 33, 31);
			}
			{
				jLabel2 = new JLabel();
				getContentPane().add(jLabel2);
				jLabel2.setText("Middle");
				jLabel2.setBounds(219, 45, 45, 31);
			}
			{
				jLabel3 = new JLabel();
				getContentPane().add(jLabel3);
				jLabel3.setText("Right");
				jLabel3.setBounds(344, 45, 36, 31);
			}
			{
				ComboBoxModel leftMFuncModel = 
					new DefaultComboBoxModel(
							new String[] { "Zoom", "Pan", "Select" });
				leftMFuncModel.setSelectedItem(cfg.getLeftMouseFunc());
				leftMFunc = new JComboBox();
				getContentPane().add(getLeftMFunc());
				leftMFunc.setModel(leftMFuncModel);
				leftMFunc.setBounds(57, 76, 92, 27);
			}
			{
				ComboBoxModel jComboBox1Model = 
					new DefaultComboBoxModel(
							new String[] { "Zoom", "Pan", "Select" });
				jComboBox1Model.setSelectedItem(cfg.getMiddleMouseFunc());
				middleMFunc = new JComboBox();
				getContentPane().add(middleMFunc);
				middleMFunc.setModel(jComboBox1Model);
				middleMFunc.setBounds(172, 76, 92, 27);
			}
			{
				ComboBoxModel jComboBox2Model = 
					new DefaultComboBoxModel(
							new String[] { "Menu", "Zoom", "Pan", "Select" });
				jComboBox2Model.setSelectedItem(cfg.getRightMouseFunc());
				rightMFunc = new JComboBox();
				getContentPane().add(rightMFunc);
				rightMFunc.setModel(jComboBox2Model);
				rightMFunc.setBounds(288, 76, 92, 27);
				rightMFunc.addActionListener(this);
			}
			{
				jLabel4 = new JLabel();
				getContentPane().add(jLabel4);
				jLabel4.setText("MouseButton");
				jLabel4.setBounds(0, 45, 103, 31);
			}

//			SpinnerNumberModel model = new SpinnerNumberModel(cfg.getAgentSize(), 10, 500, 10); 
//			JSpinner spin = addLabeledSpinner(getContentPane(), "agentSize", model);
//			spin.setMaximumSize(new Dimension(75,30));
//			spin.addChangeListener(this);
//			spin.setBounds(50, 120, 92, 27);

			jLabel4 = new JLabel();
			getContentPane().add(jLabel4);
			jLabel4.setText("AgentSize");
			jLabel4.setBounds(0, 145, 103, 31);
			JSlider slider = new JSlider();
			BoundedRangeModel model = new DefaultBoundedRangeModel(100,0,10,300);
			slider.setModel(model);
			Dictionary labels = new Hashtable();
	

			slider.setLabelTable(slider.createStandardLabels(100, 100));
			slider.setPaintLabels(true);
			slider.setBounds(0, 175, 153, 61);
			slider.addChangeListener(this);
			getContentPane().add(jLabel4);
			getContentPane().add(slider);

			
			setSize(400, 300);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stateChanged(ChangeEvent e) {
		if(e.getSource().getClass() == JSlider.class) {
			JSlider slider = (JSlider)e.getSource();
			cfg.addParam(OTFVisConfig.AGENT_SIZE, Float.toString(slider.getValue()));
			if (host != null) host.invalidateHandlers();
			System.out.println("val: "+ slider.getValue());
		}
		
	}	
	
//	static protected JSpinner addLabeledSpinner(Container c,   String label,  SpinnerModel model) 
//	{
//		JLabel l = new JLabel(label);
//		c.add(l);
//		JSpinner spinner = new JSpinner(model);
//		l.setLabelFor(spinner);
//		c.add(spinner);
//		return spinner;
//	}
//
//	public void stateChanged(ChangeEvent e) {
//		JSpinner spinner = (JSpinner)e.getSource();
//		int i = ((SpinnerNumberModel)spinner.getModel()).getNumber().intValue();
//		cfg.addParam(OTFVisConfig.AGENT_SIZE, Float.toString(i));
//		repaint();
//		//networkScrollPane.repaint();
//	}

	public JComboBox getLeftMFunc() {
		return leftMFunc;
	}

	public JComboBox getRightMFunc() {
		return rightMFunc;
	}

	public JComboBox getMiddleMFunc() {
		return middleMFunc;
	}

    public static PreferencesDialog buildMenu(final JFrame frame, OTFVisConfig config, OTFHostControlBar host) {
    	final PreferencesDialog preferencesDialog = new PreferencesDialog(frame, config,host);
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu( "File" );
        fileMenu.add("Open...");
        menuBar.add( fileMenu );
        Action exitAction = new AbstractAction() {
            { putValue( Action.NAME, "Preferences..." );
              putValue( Action.MNEMONIC_KEY, 0 ); }
            public void actionPerformed( ActionEvent e ) {
              preferencesDialog.setVisible(true);
            }
          };
          fileMenu.add( exitAction );

        frame.setJMenuBar( menuBar );
        return preferencesDialog;
	}

	public void actionPerformed(ActionEvent e) {
		JComboBox cb = (JComboBox)e.getSource();
        String newFunc = (String)cb.getSelectedItem();	
        
		if (cb == leftMFunc) {
			cfg.setLeftMouseFunc(newFunc);
		} else if ( cb == middleMFunc) {
			cfg.setMiddleMouseFunc(newFunc);
		} else if ( cb == rightMFunc) {
			cfg.setRightMouseFunc(newFunc);
		}
	}
}
