package org.matsim.contrib.util.chart;

import java.awt.Window;

import javax.swing.SwingUtilities;


/**
 * @author michalm
 */
public class SwingUtils
{
    public static void showWindow(final Window window, boolean modal)
    {
        Runnable r = new Runnable() {
            public void run()
            {
                window.pack();
                window.setVisible(true);
            }
        };

        if (modal) {
            try {
                SwingUtilities.invokeAndWait(r);
                window.dispose();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        else {
            SwingUtilities.invokeLater(r);
        }
    }
}
