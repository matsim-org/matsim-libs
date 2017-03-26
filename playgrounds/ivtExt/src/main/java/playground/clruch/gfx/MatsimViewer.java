// License: GPL. For details, see Readme.txt file.
package playground.clruch.gfx;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import playground.clruch.jmapviewer.Coordinate;
import playground.clruch.jmapviewer.JMapViewer;
import playground.clruch.jmapviewer.JMapViewerTree;
import playground.clruch.jmapviewer.OsmTileLoader;
import playground.clruch.jmapviewer.events.JMVCommandEvent;
import playground.clruch.jmapviewer.interfaces.ICoordinate;
import playground.clruch.jmapviewer.interfaces.TileLoader;
import playground.clruch.jmapviewer.interfaces.TileSource;
import playground.clruch.jmapviewer.tilesources.BingAerialTileSource;
import playground.clruch.jmapviewer.tilesources.OsmTileSource;

/**
 * Demonstrates the usage of {@link JMapViewer}
 *
 * @author Jan Peter Stotz
 */
public class MatsimViewer {
    public final JFrame jFrame = new JFrame();
    private final JMapViewerTree treeMap;
    private final JLabel zoomLabel;
    private final JLabel zoomValue;
    private final JLabel mperpLabelName;
    private final JLabel mperpLabelValue;

    /** Constructs the {@code Demo}. */
    public MatsimViewer(MatsimJMapViewer matsimJMapViewer) {

        treeMap = new JMapViewerTree(matsimJMapViewer, "Zones", false);

        getJMapViewer().addJMVListener(command -> {
            if (command.getCommand().equals(JMVCommandEvent.COMMAND.ZOOM) || command.getCommand().equals(JMVCommandEvent.COMMAND.MOVE))
                updateZoomParameters();
        });

        jFrame.setLayout(new BorderLayout());
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());
        JPanel panelTop = new JPanel();
        JPanel panelBottom = new JPanel();
        mperpLabelName = new JLabel("Meters/Pixels: ");
        mperpLabelValue = new JLabel(String.format("%s", getJMapViewer().getMeterPerPixel()));
        zoomLabel = new JLabel("Zoom: ");
        zoomValue = new JLabel(String.format("%s", getJMapViewer().getZoom()));
        jFrame.add(panel, BorderLayout.NORTH);
        jFrame.add(matsimJMapViewer.jLabel, BorderLayout.SOUTH);
        panel.add(panelTop, BorderLayout.NORTH);
        panel.add(panelBottom, BorderLayout.SOUTH);

        panelTop.add(new MatsimToggleButton(matsimJMapViewer));

        JComboBox<TileSource> tileSourceSelector = new JComboBox<>(new TileSource[] { //
                new OsmTileSource.Mapnik(), //
                new OsmTileSource.CycleMap(), //
                new BingAerialTileSource(), });
        tileSourceSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                getJMapViewer().setTileSource((TileSource) e.getItem());
            }
        });
        JComboBox<TileLoader> tileLoaderSelector;
        tileLoaderSelector = new JComboBox<>(new TileLoader[] { new OsmTileLoader(getJMapViewer()) });
        tileLoaderSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                getJMapViewer().setTileLoader((TileLoader) e.getItem());
            }
        });
        getJMapViewer().setTileLoader((TileLoader) tileLoaderSelector.getSelectedItem());
        panelTop.add(tileSourceSelector);
        panelTop.add(tileLoaderSelector);
        // ---
        {
            final JCheckBox jCheckBox = new JCheckBox("links");
            jCheckBox.setSelected(matsimJMapViewer.linkLayer.getDraw());
            jCheckBox.addActionListener(e -> matsimJMapViewer.linkLayer.setDraw(jCheckBox.isSelected()));
            panelBottom.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("veh.dest");
            jCheckBox.setSelected(matsimJMapViewer.vehicleLayer.getDrawDestinations());
            jCheckBox.addActionListener(e -> matsimJMapViewer.vehicleLayer.setDrawDestinations(jCheckBox.isSelected()));
            panelBottom.add(jCheckBox);
        }
        {
            final JCheckBox jCheckBox = new JCheckBox("req.dest");
            jCheckBox.setSelected(matsimJMapViewer.requestLayer.getDrawDestinations());
            jCheckBox.addActionListener(e -> matsimJMapViewer.requestLayer.setDrawDestinations(jCheckBox.isSelected()));
            panelBottom.add(jCheckBox);
        }
        //
        final JCheckBox showTreeLayers = new JCheckBox("Tree");
        showTreeLayers.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                treeMap.setTreeVisible(showTreeLayers.isSelected());
            }
        });
        panelBottom.add(showTreeLayers);

        final JCheckBox showTileGrid = new JCheckBox("grid");
        showTileGrid.setSelected(getJMapViewer().isTileGridVisible());
        showTileGrid.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getJMapViewer().setTileGridVisible(showTileGrid.isSelected());
            }
        });
        panelBottom.add(showTileGrid);

        panelTop.add(zoomLabel);
        panelTop.add(zoomValue);
        panelTop.add(mperpLabelName);
        panelTop.add(mperpLabelValue);
        jFrame.add(treeMap, BorderLayout.CENTER);

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
    }

    private JMapViewer getJMapViewer() {
        return treeMap.getViewer();
    }

    private void updateZoomParameters() {
        if (mperpLabelValue != null)
            mperpLabelValue.setText(String.format("%10.2e", getJMapViewer().getMeterPerPixel()));
        if (zoomValue != null)
            zoomValue.setText(String.format("%s", getJMapViewer().getZoom()));
    }

    public void setDisplayPosition(double lat, double lon, int zoom) {
        getJMapViewer().setDisplayPosition(new Point(), new Coordinate(lat, lon), zoom);

    }

}
