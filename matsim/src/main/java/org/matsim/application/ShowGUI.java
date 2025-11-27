package org.matsim.application;

import org.matsim.run.gui.Gui;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@CommandLine.Command(name = "gui", description = "Run the scenario through the MATSim GUI")
class ShowGUI implements Callable<Integer> {

    @CommandLine.ParentCommand
    private MATSimApplication parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {

        String name = "MATSim GUI";
        if (spec.parent() != null && spec.parent().usageMessage().header().length > 0) {
            // Use header of parent and cutoff formatting
            name = spec.parent().usageMessage().header()[0];
            name = name.substring(MATSimApplication.COLOR.length(), name.length() - 4);
        }

		File configFile = null;

		// Try to load default config file
		if (parent.getDefaultScenario() != null && new File(parent.getDefaultScenario()).exists())
			configFile = new File(parent.getDefaultScenario());

		// override the default if present
		if (parent.getConfigPath() != null)
			configFile = new File(parent.getConfigPath());

        Future<Gui> f = Gui.show(name, parent.getClass(), configFile);

        Gui gui = f.get();

		// Set the current working directory to be used in the gui, when run from the command line
		// If the gui is run from desktop, the working directory is not overwritten

		// Assumption is that starting something from command line, the user expects that the working directory remains the same
		if (!System.getProperty("MATSIM_GUI_DESKTOP", "false").equals("true"))
			gui.setWorkingDirectory(new File(""));

        while (gui.isShowing())
            Thread.sleep(250);

        return 0;
    }

    public static void main(String[] args) {
        new CommandLine(new ShowGUI()).execute(args);
    }

}
