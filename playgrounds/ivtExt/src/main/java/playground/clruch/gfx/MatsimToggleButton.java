// code by jph
package playground.clruch.gfx;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JToggleButton;

import ch.ethz.idsc.queuey.view.util.net.ObjectClient;
import ch.ethz.idsc.queuey.view.util.net.ObjectHandler;
import playground.clruch.net.SimulationObject;
import playground.clruch.net.SimulationServer;

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
                        client = new ObjectClient("localhost", SimulationServer.OBJECT_SERVER_PORT, new ObjectHandler() {
                            @Override
                            public void handle(Object object) {
                                matsimJMapViewer.setSimulationObject((SimulationObject) object);
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
