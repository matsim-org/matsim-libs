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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

import org.matsim.api.core.v01.Coord;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import playground.clruch.jmapviewer.Coordinate;
import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.jmapviewer.interfaces.ICoordinate;
import playground.clruch.net.DummyStorageSupplier;
import playground.clruch.net.IterationFolder;
import playground.clruch.net.StorageSupplier;
import playground.clruch.net.StorageUtils;
import playground.clruch.utils.gui.RowPanel;
import playground.clruch.utils.gui.SpinnerLabel;

/**
 * Demonstrates the usage of {@link JMapViewer}
 *
 * adapted from code by Jan Peter Stotz
 */
public class MatsimViewerFrame implements Runnable {
    public int STEPSIZE_SECONDS = 10; // TODO this should be derived from storage files
    // ---
    private final MatsimMapComponent matsimMapComponent;
    private boolean isLaunched = true;
    private final JToggleButton jToggleButton = new JToggleButton("auto");
    private final JSlider jSlider = new JSlider(0, 1, 0);
    private Scalar playbackSpeed = RealScalar.of(50);
    private final Thread thread;

    public final JFrame jFrame = new JFrame();
    StorageSupplier storageSupplier = new DummyStorageSupplier();

    public static FlowLayout createFlowLayout() {
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        return flowLayout;
    }

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
        JPanel panelControls = new JPanel(createFlowLayout());
        panelNorth.add(panelControls, BorderLayout.NORTH);

        jFrame.add(panelNorth, BorderLayout.NORTH);
        {
            RowPanel rowPanel = new RowPanel();
            for (ViewerLayer viewerLayer : matsimMapComponent.viewerLayers)
                rowPanel.add(viewerLayer.createPanel());
            JPanel jPanel = new JPanel(new BorderLayout());
            jPanel.add(rowPanel.jPanel, BorderLayout.NORTH);
            JScrollPane jScrollPane = new JScrollPane(jPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            jScrollPane.setPreferredSize(new Dimension(150, 0));
            jFrame.add(jScrollPane, BorderLayout.EAST);
        }
        jFrame.add(matsimMapComponent, BorderLayout.CENTER);
        jFrame.add(matsimMapComponent.jLabel, BorderLayout.SOUTH);

        MatsimToggleButton matsimToggleButton = new MatsimToggleButton(matsimMapComponent);
        matsimToggleButton.addActionListener(l -> jSlider.setEnabled(!matsimToggleButton.isSelected()));
        panelControls.add(matsimToggleButton);

        panelNorth.add(jSlider, BorderLayout.SOUTH);
        {
            List<IterationFolder> list = StorageUtils.getAvailableIterations();
            if (list.isEmpty()) {
                panelControls.add(new JLabel("no playback"));
            } else {
                {
                    SpinnerLabel<IterationFolder> spinnerLabel = new SpinnerLabel<>();
                    spinnerLabel.setList(list);
                    IterationFolder last = list.get(list.size() - 1);
                    storageSupplier = last.storageSupplier;
                    spinnerLabel.setValueSafe(last);
                    jSlider.setMaximum(storageSupplier.size() - 1);
                    spinnerLabel.addSpinnerListener(i -> {
                        storageSupplier = i.storageSupplier;
                        jSlider.setMaximum(storageSupplier.size() - 1);
                        updateFromStorage(jSlider.getValue());
                    });
                    spinnerLabel.addToComponentReduced(panelControls, new Dimension(50, 28), "iteration");
                }
                jSlider.addChangeListener(e -> updateFromStorage(jSlider.getValue()));
                {
                    JButton jButton = new JButton("<");
                    jButton.addActionListener(e -> jSlider.setValue(jSlider.getValue() - 1));
                    panelControls.add(jButton);
                }
                {
                    JButton jButton = new JButton(">");
                    jButton.addActionListener(e -> jSlider.setValue(jSlider.getValue() + 1));
                    panelControls.add(jButton);
                }
                panelControls.add(jToggleButton);
                {
                    SpinnerLabel<Scalar> spinnerLabel = new SpinnerLabel<>();
                    spinnerLabel.setArray( //
                            RealScalar.of(200), //
                            RealScalar.of(100), //
                            RealScalar.of(50), //
                            RealScalar.of(25), //
                            RealScalar.of(10), //
                            RealScalar.of(5), //
                            RealScalar.of(2), //
                            RealScalar.of(1) //
                    );
                    spinnerLabel.setValueSafe(playbackSpeed);
                    spinnerLabel.addSpinnerListener(i -> playbackSpeed = i);
                    spinnerLabel.addToComponentReduced(panelControls, new Dimension(50, 28), "playback factor");
                }
            }
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
        thread = new Thread(this);
        thread.start();
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
            if (jSlider != null && jToggleButton.isSelected()) {
                jSlider.setValue(jSlider.getValue() + 1);
                millis = RealScalar.of(1000 * STEPSIZE_SECONDS).divide(playbackSpeed).number().intValue();
            }
            try {
                Thread.sleep(millis);
            } catch (Exception e) {
            }
        }

    }

}
