package josmMatsimPlugin;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * the filter dialog
 * @author nkuehnel
 * 
 */
public class FilterDialog extends JPanel
{
	private JOptionPane optionPane;

	public FilterDialog()
	{
		
	}
	
	
	public void setOptionPane(JOptionPane optionPane)
	{
		this.optionPane = optionPane;
	}

}
