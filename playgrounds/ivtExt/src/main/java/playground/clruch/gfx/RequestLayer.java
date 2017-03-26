package playground.clruch.gfx;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import playground.clruch.net.OsmLink;
import playground.clruch.net.RequestContainer;
import playground.clruch.net.SimulationObject;

public class RequestLayer extends ViewerLayer {

    private static Font requestsFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    // ---
    private volatile boolean drawRequestDestinations = true;

    public RequestLayer(MatsimJMapViewer matsimJMapViewer) {
        super(matsimJMapViewer);
    }

    double maxWaitTime;

    @Override
    void paint(Graphics2D graphics, SimulationObject ref) {
        maxWaitTime = 0;
        // draw requests
        graphics.setFont(requestsFont);
        Map<Integer, List<RequestContainer>> map = //
                ref.requests.stream().collect(Collectors.groupingBy(rc -> rc.fromLinkId));
        for (Entry<Integer, List<RequestContainer>> entry : map.entrySet()) {
            Point p1;
            {
                int linkId = entry.getKey();
                OsmLink osmLink = matsimJMapViewer.db.getOsmLink(linkId);
                p1 = matsimJMapViewer.getMapPosition(osmLink.getAt(0.5));
            }
            if (p1 != null) {
                final int numRequests = entry.getValue().size();

                final int x = p1.x;
                final int y = p1.y;

                {
                    graphics.setColor(new Color(32, 128, 32, 128));
                    int index = numRequests;
                    for (RequestContainer rc : entry.getValue()) {
                        double waitTime = ref.now - rc.submissionTime;
                        maxWaitTime = Math.max(waitTime, maxWaitTime);
                        int piy = y - index;
                        int wid = (int) waitTime / 10;
                        int left = x - wid / 2;
//                        graphics.drawLine(left, piy, left + wid, piy);
                        --index;
                    }
                }
                if (drawRequestDestinations) {
                    graphics.setColor(new Color(128, 128, 128, 64));
                    for (RequestContainer rc : entry.getValue()) {
                        int linkId = rc.toLinkId;
                        OsmLink osmLink = matsimJMapViewer.db.getOsmLink(linkId);
                        Point p2 = matsimJMapViewer.getMapPositionAlways(osmLink.getAt(0.5));
                        graphics.drawLine(x, y, p2.x, p2.y);
                    }
                }
                graphics.setColor(Color.DARK_GRAY);
                graphics.drawString("" + numRequests, x, y - numRequests);
            }
        }

    }

    @Override
    void hud(Graphics2D graphics, SimulationObject ref) {
        {
            InfoString infoString = new InfoString(String.format("%5d %s", ref.requests.size(), "open requests"));
            infoString.color = Color.BLACK; // new Color(204, 122, 0);
            matsimJMapViewer.append(infoString);
        }
        {
            InfoString infoString = new InfoString(String.format("%5d %s", Math.round(maxWaitTime / 60), "maxWaitTime [min]"));
            infoString.color = Color.BLACK; // new Color(255, 102, 0);
            matsimJMapViewer.append(infoString);
        }
        matsimJMapViewer.append("%5d %s", ref.total_matchedRequests, "matched req.");
        matsimJMapViewer.appendSeparator();
    }

    public void setDrawDestinations(boolean selected) {
        drawRequestDestinations = selected;
        matsimJMapViewer.repaint();
    }

    public boolean getDrawDestinations() {
        return drawRequestDestinations;
    }

}
