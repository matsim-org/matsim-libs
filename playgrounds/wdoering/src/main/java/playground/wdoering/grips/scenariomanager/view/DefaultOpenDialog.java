package playground.wdoering.grips.scenariomanager.view;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import playground.wdoering.grips.scenariomanager.control.Controller;

public class DefaultOpenDialog extends JFileChooser
{
	protected Controller controller;
	public DefaultOpenDialog(Controller controller, final String fileExtension, final String fileDescription, boolean mandatory)
	{
		this.controller = controller;
		this.setFileFilter(new FileFilter()
		{
			
			@Override
			public String getDescription()
			{
				return fileDescription;
			}
			
			@Override
			public boolean accept(File f)
			{
				if ((f.toString().endsWith(fileExtension)) || (f.isDirectory()))
					return true;
				else
					return false;
			}
		});
		this.setCurrentDirectory(controller.getCurrentWorkingDirectory());
	}

}
