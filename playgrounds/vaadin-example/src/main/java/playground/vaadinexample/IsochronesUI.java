package playground.vaadinexample;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.*;
import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.*;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.vaadin.addon.leaflet.*;
import org.vaadin.addon.leaflet.util.JTSUtil;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

@Theme("mytheme")
@SuppressWarnings("serial")
public class IsochronesUI extends UI {

    private MathTransform coordinateTransformation;
    private LLayerGroup isochronesLayer;
    private LLayerGroup nodesLayer;
    private HierarchicalContainer container;
    private LMap map;
    private Network network;

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = IsochronesUI.class, widgetset = "playground.vaadinexample.AppWidgetSet")
    public static class Servlet extends VaadinServlet {
    }

    enum ContainerProperties {
        TRAVEL_TIME, AREA, N_GEOMETRIES;
    }

    ContourService contourService;

    @Override
    protected void init(VaadinRequest request) {
        network = NetworkUtils.createNetwork();
        try {
            new MatsimNetworkReader(network).parse(new URL("https://raw.githubusercontent.com/matsim-org/matsimExamples/master/countries/ke/nairobi/2015-10-15_network.xml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CoordinateReferenceSystem sourceCRS = MGC.getCRS("EPSG:21037");
        CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84);

        try {
            coordinateTransformation = CRS.findMathTransform(sourceCRS, targetCRS, true);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }

        map = new LMap();
        LTileLayer osmTiles = new LTileLayer("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png");
        osmTiles.setAttributionString("© OpenStreetMap Contributors");

        LLayerGroup originLayer = new LLayerGroup();
        final Node startNode = network.getNodes().values().iterator().next();
        try {
            final LMarker origin = new LMarker((Point) JTS.transform(MGC.coord2Point(startNode.getCoord()), coordinateTransformation));
            origin.addDragEndListener(new LMarker.DragEndListener() {
                @Override
                public void dragEnd(LMarker.DragEndEvent dragEndEvent) {
                    try {
                        Point point = (Point) JTS.transform(origin.getGeometry(), coordinateTransformation.inverse());
                        Node nearestNode = ((NetworkImpl) network).getNearestNode(MGC.coordinate2Coord(point.getCoordinate()));
                        compute(nearestNode);
                    } catch (TransformException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            originLayer.addComponent(origin);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }

        nodesLayer = new LLayerGroup();
        isochronesLayer = new LLayerGroup();

        map.addBaseLayer(osmTiles, "OSM");
        map.addOverlay(originLayer, "Origin");
        map.addOverlay(nodesLayer, "Nodes");
        map.addOverlay(isochronesLayer, "Isochrones");

        container = new HierarchicalContainer();
        container.addContainerProperty(ContainerProperties.TRAVEL_TIME, Double.class, 0.0);
        container.addContainerProperty(ContainerProperties.AREA, Double.class, 0.0);
        container.addContainerProperty(ContainerProperties.N_GEOMETRIES, Integer.class, 0);

        TreeTable table = new TreeTable("Isochrones", container);
        table.setColumnHeader(ContainerProperties.TRAVEL_TIME, "Travel Time [s]");
        table.setColumnHeader(ContainerProperties.AREA, "Area [m^2]");
        table.setColumnHeader(ContainerProperties.N_GEOMETRIES, "Number of sub-geometries");

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);

        HorizontalLayout options = new HorizontalLayout();

        // I need my own checkbox for turning on and off nodes. The box in the Leaflet
        // control is only for the client side - the nodes are still all transferred to
        // the browser when that checkbox is turned off.
        CheckBox nodes = new CheckBox("Nodes");
        nodes.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                if ((Boolean) event.getProperty().getValue()) {
                    Collection<NodeWithCost> nodes = contourService.getNodes();
                    for (final NodeWithCost node : nodes) {
                        try {
                            LCircleMarker layer = new LCircleMarker((Point) JTS.transform(node.getGeometry(), coordinateTransformation), 2.0);
                            layer.setPopup(String.format("z: %.2f", node.getTime()));
                            nodesLayer.addComponent(layer);
                        } catch (TransformException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    nodesLayer.removeAllComponents();
                }
            }
        });
        options.addComponent(nodes);

        layout.addComponents(options, map, table);
        layout.setExpandRatio(map, 0.8f);
        layout.setExpandRatio(table, 0.2f);
        table.setSizeFull();
        layout.setSizeFull();
        setContent(layout);

        compute(startNode);
        map.zoomToContent();
    }

    private void compute(Node startNode) {
        container.removeAllItems();
        nodesLayer.removeAllComponents();
        isochronesLayer.removeAllComponents();

        contourService = new ContourService(network, startNode);

        Collection<Contour> contours = new ArrayList<>();
        contours.add(contourService.getContour(10.0 * 60.0, "green"));
        contours.add(contourService.getContour(20.0 * 60.0, "yellow"));
        contours.add(contourService.getContour(30.0 * 60.0, "red"));

        for (final Contour contour : contours) {
            Object contourId = container.addItem();
            Item contourItem = container.getItem(contourId);
            contourItem.getItemProperty(ContainerProperties.TRAVEL_TIME).setValue(contour.getZ());
            contourItem.getItemProperty(ContainerProperties.AREA).setValue(contour.getArea());
            contourItem.getItemProperty(ContainerProperties.N_GEOMETRIES).setValue(contour.getNGeometries());
            MultiPolygon geometry = ((MultiPolygon) contour.getGeometry());
            for (int i=0; i < geometry.getNumGeometries(); i++) {
                final Polygon polygon = (Polygon) geometry.getGeometryN(i);
                Object polygonId = container.addItem();
                Item polygonItem = container.getItem(polygonId);
                polygonItem.getItemProperty(ContainerProperties.TRAVEL_TIME).setValue(contour.getZ());
                polygonItem.getItemProperty(ContainerProperties.AREA).setValue(polygon.getArea());
                polygonItem.getItemProperty(ContainerProperties.N_GEOMETRIES).setValue(1 + polygon.getNumInteriorRing());
                Object shellId = container.addItem();
                Item shellItem = container.getItem(shellId);
                shellItem.getItemProperty(ContainerProperties.TRAVEL_TIME).setValue(contour.getZ());
                shellItem.getItemProperty(ContainerProperties.AREA).setValue(Math.abs(CGAlgorithms.signedArea(polygon.getExteriorRing().getCoordinates())));
                shellItem.getItemProperty(ContainerProperties.N_GEOMETRIES).setValue(1);
                container.setParent(shellId, polygonId);
                container.setChildrenAllowed(shellId, false);
                for (int j=0; j < polygon.getNumInteriorRing(); j++) {
                    final LinearRing hole = (LinearRing) polygon.getInteriorRingN(j);
                    Object holeId = container.addItem();
                    Item holeItem = container.getItem(holeId);
                    holeItem.getItemProperty(ContainerProperties.TRAVEL_TIME).setValue(contour.getZ());
                    holeItem.getItemProperty(ContainerProperties.AREA).setValue(- Math.abs(CGAlgorithms.signedArea(hole.getCoordinates())));
                    holeItem.getItemProperty(ContainerProperties.N_GEOMETRIES).setValue(1);
                    container.setParent(holeId, polygonId);
                    container.setChildrenAllowed(holeId, false);
                }
                container.setParent(polygonId, contourId);
                try {
                    LPolygon lPolygon = JTSUtil.toPolygon((Polygon) JTS.transform(polygon, coordinateTransformation));
                    lPolygon.setColor(contour.getColor());
                    lPolygon.setFill(false);
                    lPolygon.addClickListener(new LeafletClickListener() {
                        @Override
                        public void onClick(LeafletClickEvent event) {
                            // Polygons are clickable
                            double area = polygon.getArea();
                            Notification contourInfo = new Notification(Double.toString(area), "Area");
                            contourInfo.show(Page.getCurrent());
                        }
                    });
                    isochronesLayer.addComponent(lPolygon);
                } catch (TransformException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

}
