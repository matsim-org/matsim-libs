package josmMatsimPlugin;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * the filter dialog
 * @author nkuehnel
 * 
 */
public class MATSimFilterDialog extends JPanel
{
	private JOptionPane optionPane;

	public MATSimFilterDialog()
	{
		
	}
	
	
	public void setOptionPane(JOptionPane optionPane)
	{
		this.optionPane = optionPane;
	}

}
