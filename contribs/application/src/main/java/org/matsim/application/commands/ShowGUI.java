package org.matsim.application.commands;

import org.matsim.application.MATSimApplication;
import org.matsim.run.gui.Gui;
import picocli.CommandLine;

import javax.swing.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "gui", description = "Show the graphical user interface")
public class ShowGUI implements Callable<Integer> {

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

        Gui.show(name, MATSimApplication.class);

        return 0;
    }

    public static void main(String[] args) {
        new CommandLine(new ShowGUI()).execute(args);
    }

}
