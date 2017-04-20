package playground.clruch.gfx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

class SbbClockDisplay {

    int wid = 52;
    int hourRad = 32;
    int hourRadIn = 7;
    int minRad = 45;
    int minRadAl = 8;
    int minRadOut = 48;
    int minRadIn = 40;
    int secRadOut = 32;
    int secRadIn = 15;
    int secCirc = 10;

    void drawClock(Graphics2D graphics, long now, Point c) {

        SecondsToHMS hms = new SecondsToHMS(now);
        // code to draw the clock was stolen from https://processing.org/examples/clock.html
        // code adapted by jph
        graphics.setColor(Color.WHITE);
        graphics.fillArc(c.x - wid, c.y - wid, 2 * wid, 2 * wid, 0, 360);

        final double h = ((hms.h + hms.m / 60.0) / 12.0) * 2 * Math.PI;
        final double m = ((hms.m + hms.s / 60.0) / 60.0) * 2 * Math.PI;
        final double s = hms.s / 60.0 * 2 * Math.PI;

        // Draw the hands of the clock
        // graphics2d.drawLine(c.x, c.y, c.x + Math.cos(m) * minutesRadius, cy + sin(m) * minutesRadius);

        graphics.setColor(Color.BLACK); // new Color(128, 128, 128, 255)
        Stroke stroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(4));
        {
            double dx = +Math.sin(h);
            double dy = -Math.cos(h);
            double cx = c.x + dx * hourRad;
            double cy = c.y + dy * hourRad;
            graphics.drawLine( //
                    (int) (c.x - hourRadIn * dx), //
                    (int) (c.y - hourRadIn * dy), //
                    (int) cx, //
                    (int) cy);
        }

        graphics.setStroke(new BasicStroke(3));
        {
            double dx = +Math.sin(m);
            double dy = -Math.cos(m);
            double cx = c.x + dx * minRad;
            double cy = c.y + dy * minRad;
            graphics.drawLine( //
                    (int) (c.x - minRadAl * dx), //
                    (int) (c.y - minRadAl * dy), //
                    (int) cx, //
                    (int) cy);
        }

        // Draw the minute ticks
        graphics.setStroke(new BasicStroke(2));
        for (int a = 0; a < 360; a += 6)
            secAt(graphics, c, a);
        graphics.setStroke(new BasicStroke(3));
        for (int a = 0; a < 360; a += 6 * 5)
            dotAt(graphics, c, a);

        graphics.setColor(Color.RED); // new Color(128, 128, 128, 255)
        graphics.setStroke(new BasicStroke(1.5f));
        {
            double dx = +Math.sin(s);
            double dy = -Math.cos(s);
            double cx = c.x + dx * secRadOut;
            double cy = c.y + dy * secRadOut;
            graphics.drawLine( //
                    (int) (c.x - secRadIn * dx), //
                    (int) (c.y - secRadIn * dy), //
                    (int) cx, //
                    (int) cy);
            graphics.fillArc((int) cx - secCirc / 2, (int) cy - secCirc / 2, secCirc, secCirc, 0, 360);
        }

        graphics.setStroke(stroke);
    }

    void dotAt(Graphics2D graphics, Point c, int a) {
        double angle = a * Math.PI / 180;
        double x1 = c.x + Math.cos(angle) * minRadOut;
        double y1 = c.y + Math.sin(angle) * minRadOut;
        double x2 = c.x + Math.cos(angle) * minRadIn;
        double y2 = c.y + Math.sin(angle) * minRadIn;
        graphics.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
    }

    void secAt(Graphics2D graphics, Point c, int a) {
        double angle = a * Math.PI / 180;
        double x1 = c.x + Math.cos(angle) * minRadOut;
        double y1 = c.y + Math.sin(angle) * minRadOut;
        double x2 = c.x + Math.cos(angle) * (minRadOut - 1);
        double y2 = c.y + Math.sin(angle) * (minRadOut - 1);
        graphics.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
    }

}
