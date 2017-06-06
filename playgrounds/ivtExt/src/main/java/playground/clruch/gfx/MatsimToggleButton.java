// code by jph
package playground.clruch.gfx;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JToggleButton;

import playground.clruch.net.ObjectClient;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationSubscriber;

/* package */ class MatsimToggleButton extends JToggleButton {

    ObjectClient client = null;

    public MatsimToggleButton(MatsimMapComponent matsimJMapViewer) {
        super("connect...");

        addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                if (isSelected()) {
                    try {
                        if (client != null) {
                            client.close();
                            client = null;
                        }
                        client = new ObjectClient("localhost", new SimulationSubscriber() {
                            @Override
                            public void handle(SimulationObject simulationObject) {
                                matsimJMapViewer.setSimulationObject(simulationObject);
                            }
                        });
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                } else {
                    if (client != null) {
                        client.close();
                        client = null;
                    }
                }
            }
        });
    }
}
