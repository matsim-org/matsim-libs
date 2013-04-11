package playground.wdoering.grips.scenariomanager.view;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import playground.wdoering.grips.scenariomanager.control.Controller;

public class DefaultSaveDialog extends JFileChooser
{
	protected Controller controller;
	public DefaultSaveDialog(Controller controller, final String fileExtension, final String fileDescription, boolean mandatory)
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
				if (f.toString().endsWith(fileExtension))
					return true;
				else
					return false;
			}
		});
		this.setCurrentDirectory(controller.getCurrentWorkingDirectory());
	}

	/*
	 final JFileChooser fc = new JFileChooser();
				fc.setCurrentDirectory(new File(currentDirectory));

				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				fc.setFileFilter(new FileFilter() {

					@Override
					public String getDescription() {
						return "choose directory for the TIFF export";
					}

					@Override
					public boolean accept(File f) {
						if (f.isDirectory())
							return true;
						else
							return false;
					}
				});

				int returnVal = fc.showSaveDialog(this.frame);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
	 */
}
