package org.matsim.application.commands;

import org.matsim.application.MATSimApplication;
import org.matsim.run.gui.Gui;
import picocli.CommandLine;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@CommandLine.Command(name = "gui", description = "Show the graphical user interface")
public class ShowGUI implements Callable<Integer> {

    @CommandLine.ParentCommand
    private MATSimApplication parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {

        String name = "MATSim GUI";
        if (spec.parent() != null) {
            // Use header of parent and cutoff formatting
            name = spec.parent().usageMessage().header()[0];
            name = name.substring(MATSimApplication.COLOR.length(), name.length() - 4);
        }

        Future<Gui> f = Gui.show(name, parent.getClass());

        Gui gui = f.get();

        while (gui.isShowing())
            Thread.sleep(250);

        return 0;
    }

    public static void main(String[] args) {
        new CommandLine(new ShowGUI()).execute(args);
    }

}
