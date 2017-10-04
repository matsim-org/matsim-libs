// code adapted by jph
// License: GPL. For details, see Readme.txt file.
package playground.clruch.gfx;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.queuey.view.jmapviewer.Coordinate;
import ch.ethz.idsc.queuey.view.jmapviewer.JMapViewer;
import ch.ethz.idsc.queuey.view.jmapviewer.interfaces.ICoordinate;
import ch.ethz.idsc.queuey.view.util.gui.RowPanel;
import ch.ethz.idsc.queuey.view.util.gui.SpinnerLabel;
import ch.ethz.idsc.tensor.RealScalar;
import playground.clruch.net.DummyStorageSupplier;
import playground.clruch.net.IterationFolder;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.StorageUtils;

/**
 * Demonstrates the usage of {@link JMapViewer}
 *
 * adapted from code by Jan Peter Stotz
 */
public class MatsimViewerFrame implements Runnable {
    public static int STEPSIZE_SECONDS = 10; // TODO this should be derived from storage files
    // ---
    private final MatsimMapComponent matsimMapComponent;
    private boolean isLaunched = true;
    private final JToggleButton jToggleButtonAuto = new JToggleButton("auto");
    private int playbackSpeed = 50;
    private final Thread thread;

    public final JFrame jFrame = new JFrame();
    StorageSupplier storageSupplier = new DummyStorageSupplier();

    SpinnerLabel<IterationFolder> spinnerLabelIter = new SpinnerLabel<>();
    JButton jButtonDecr = new JButton("<");
    JButton jButtonIncr = new JButton(">");
    SpinnerLabel<Integer> spinnerLabelSpeed = new SpinnerLabel<>();
    private final JSlider jSlider = new JSlider(0, 1, 0);

    /** Constructs the {@code Demo}. */
    public MatsimViewerFrame(MatsimMapComponent matsimMapComponent) {
        this.matsimMapComponent = matsimMapComponent;
        // ---
        jFrame.setTitle("ETH Z\u00fcrich MATSim Viewer");
        jFrame.setLayout(new BorderLayout());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                isLaunched = false;
                thread.interrupt();
            }
        });
        JPanel panelNorth = new JPanel(new BorderLayout());
        // JPanel panelControls = new JPanel(createFlowLayout());
        JToolBar panelControls = new JToolBar();
        panelControls.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        panelControls.setFloatable(false);
        JToolBar panelConfig = new JToolBar();
        panelConfig.setFloatable(false);
        JPanel panelToprow = new JPanel(new BorderLayout());
        panelToprow.add(panelControls, BorderLayout.CENTER);
        panelToprow.add(panelConfig, BorderLayout.EAST);
        panelNorth.add(panelToprow, BorderLayout.NORTH);
        panelNorth.add(jSlider, BorderLayout.SOUTH);
        jFrame.add(panelNorth, BorderLayout.NORTH);

        JScrollPane jScrollPane;
        {
            RowPanel rowPanel = new RowPanel();
            for (ViewerLayer viewerLayer : matsimMapComponent.viewerLayers)
                rowPanel.add(viewerLayer.createPanel());
            JPanel jPanel = new JPanel(new BorderLayout());
            jPanel.add(rowPanel.jPanel, BorderLayout.NORTH);
            jScrollPane = new JScrollPane(jPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            jScrollPane.setPreferredSize(new Dimension(150, 0));
            jFrame.add(jScrollPane, BorderLayout.EAST);
        }
        jFrame.add(matsimMapComponent, BorderLayout.CENTER);
        jFrame.add(matsimMapComponent.jLabel, BorderLayout.SOUTH);

        jSlider.addChangeListener(e -> updateFromStorage(jSlider.getValue()));

        MatsimToggleButton matsimToggleButton = new MatsimToggleButton(matsimMapComponent);
        matsimToggleButton.addActionListener(l -> jSlider.setEnabled(!matsimToggleButton.isSelected()));
        panelControls.add(matsimToggleButton);
        {
            JButton jButton = new JButton("reindex");
            jButton.setToolTipText("reindex available simulation objects");
            jButton.addActionListener(event -> reindex());
            panelControls.add(jButton);
        }
        panelControls.addSeparator();
        {
            spinnerLabelIter.addSpinnerListener(i -> {
                storageSupplier = i.storageSupplier;
                jSlider.setMaximum(storageSupplier.size() - 1);
                updateFromStorage(jSlider.getValue());
            });
            spinnerLabelIter.addToComponentReduced(panelControls, new Dimension(50, 26), "iteration");
        }
        panelControls.addSeparator();
        {
            jButtonDecr.addActionListener(e -> jSlider.setValue(jSlider.getValue() - 1));
            panelControls.add(jButtonDecr);
        }
        {
            jButtonIncr.addActionListener(e -> jSlider.setValue(jSlider.getValue() + 1));
            panelControls.add(jButtonIncr);
        }
        {

            spinnerLabelSpeed.setArray( //
                    800, 500, 400, 300, 200, 150, 125, 100, //
                    75, 50, 25, 10, 5, 2, 1);
            spinnerLabelSpeed.setValueSafe(playbackSpeed);
            spinnerLabelSpeed.addSpinnerListener(i -> playbackSpeed = i);
            spinnerLabelSpeed.addToComponentReduced(panelControls, new Dimension(50, 26), "playback factor");
        }
        {
            panelControls.add(jToggleButtonAuto);
        }
        // {
        // JToggleButton jToggleButton = new JToggleButton("full");
        // jToggleButton.setSelected(false);
        // jToggleButton.addActionListener(event -> {
        // boolean full = jToggleButton.isSelected();
        // if (full)
        // jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        // jFrame.validate();
        // });
        // panelConfig.add(jToggleButton);
        // }
        {
            JToggleButton jToggleButton = new JToggleButton("config");
            jToggleButton.setSelected(true);
            jToggleButton.addActionListener(event -> {
                boolean show = jToggleButton.isSelected();
                jScrollPane.setVisible(show);
                matsimMapComponent.jLabel.setVisible(show);
                jFrame.validate();
            });
            panelConfig.add(jToggleButton);
        }

        getJMapViewer().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ICoordinate asd = getJMapViewer().getPosition(e.getPoint());
                System.out.println(asd.getLat() + "  " + asd);

                Point p = getJMapViewer().getMapPosition(asd.getLat(), asd.getLon());
                System.out.println(p);
            }
        });
        getJMapViewer().setZoomContolsVisible(false);

        getJMapViewer().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                boolean cursorHand = getJMapViewer().getAttribution().handleAttributionCursor(p);
                if (cursorHand) {
                    getJMapViewer().setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    getJMapViewer().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                // if (showToolTip.isSelected())
                // getJMapViewer().setToolTipText(getJMapViewer().getPosition(p).toString());
            }
        });
        reindex();
        thread = new Thread(this);
        thread.start();
    }

    void reindex() {
        // System.out.println("reindex");
        List<IterationFolder> list = StorageUtils.getAvailableIterations();
        boolean nonEmpty = !list.isEmpty();
        spinnerLabelIter.setEnabled(nonEmpty);
        jButtonDecr.setEnabled(nonEmpty);
        jButtonIncr.setEnabled(nonEmpty);
        spinnerLabelSpeed.setEnabled(nonEmpty);
        jSlider.setEnabled(nonEmpty);
        jToggleButtonAuto.setEnabled(nonEmpty);
        if (!nonEmpty)
            jToggleButtonAuto.setSelected(false);

        if (nonEmpty) {
            spinnerLabelIter.setList(list);
            IterationFolder last = list.get(list.size() - 1);
            storageSupplier = last.storageSupplier;
            spinnerLabelIter.setValueSafe(last);
            jSlider.setMaximum(storageSupplier.size() - 1);
        }
    }

    void updateFromStorage(int index) {
        try {
            matsimMapComponent.simulationObject = storageSupplier.getSimulationObject(index);
            matsimMapComponent.repaint();
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("cannot load: " + index);
        }
    }

    private JMapViewer getJMapViewer() {
        return matsimMapComponent;
    }

    public void setDisplayPosition(Coord coord, int zoom) {
        // double[] bb = NetworkUtils.getBoundingBox(network.getNodes().values());
        // System.out.println(bb[0] + " " + bb[1] + " " + bb[2] + " " + bb[3]);
        setDisplayPosition(coord.getY(), coord.getX(), zoom);
    }

    public void setDisplayPosition(double lat, double lon, int zoom) {
        getJMapViewer().setDisplayPosition(new Point(), new Coordinate(lat, lon), zoom);
    }

    @Override
    public void run() {
        while (isLaunched) {
            int millis = 500;
            if (jSlider != null && jToggleButtonAuto.isSelected()) {
                jSlider.setValue(jSlider.getValue() + 1);
                millis = RealScalar.of(1000 * STEPSIZE_SECONDS).divide(RealScalar.of(playbackSpeed)).number().intValue();
            }
            try {
                Thread.sleep(millis);
            } catch (Exception e) {
            }
        }

    }

}
