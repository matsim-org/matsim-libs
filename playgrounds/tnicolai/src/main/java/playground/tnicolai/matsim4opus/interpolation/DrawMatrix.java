// Einstieg in die Informatik mit Java, Gerd Bohlender, Uni Karlsruhe, 14.11.07
// Grafische Ausgabe von Matrizen in Frame

//Quelle: www.rz.uni-karlsruhe.de/~ae15/java/beispiele/DrawMatrix.java, Zugriff 11.04.12, 14:13 Uhr

package playground.tnicolai.matsim4opus.interpolation;

import java.awt.*;
import java.awt.event.*;


public class DrawMatrix {
  private Frame frame;
  private MyCanvas myCanvas;
  int width, height; // Koordinaten des Frames! Canvas ist etwas kleiner!
  double [][] matrix; // diese Matrix soll visualisiert werden
  double minValue, maxValue; // Wertebereich der Matrixelemente
  Color minColor, midColor, maxColor; // zugeordnete Farben
  
  // Konstruktoren
  public DrawMatrix (int xSize, int ySize, int xPos, int yPos, 
    String title, double min, double max, Color minC, Color midC, Color maxC) {
    frame = new Frame (title);
    myCanvas = new MyCanvas ();
    myCanvas.setSize (xSize, ySize);
    frame.add (myCanvas);
    frame.setSize (xSize, ySize);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing (WindowEvent e) {
        // System.exit (0);
        frame.dispose ();
        // myCanvas.dispose ();
      }
    });
    minValue = min;
    maxValue = max;
    minColor = minC;
    midColor = midC;
    maxColor = maxC;
    frame.setLocation (xPos, yPos);
    frame.setFocusable (false);
    frame.setVisible (true);
    // Rahmengroesse bestimmen
    Insets insets = frame.getInsets ();
    frame.setSize (xSize+insets.left+insets.right, ySize+insets.top+insets.bottom);
    frame.setVisible (true);
    // System.out.println (insets.left + " " + insets.top);
  }

  public DrawMatrix (int xSize, int ySize, int xPos, int yPos, 
    String title, double min, double max) {
    this (xSize, ySize, xPos, yPos, title, min, max, Color.blue, Color.green, Color.red);
  }
  
  // draw matrix
  public void draw (double[][] m) {
    matrix = m;
    myCanvas.repaint();
  }

  // Frame schliessen
  public void stop () {
    frame.dispose ();
  }



  // innere Klasse mit Zeichenoberflaeche
  private class MyCanvas extends Canvas {
    private Color bgColor;

    public MyCanvas () {
      bgColor = Color.black;
    }

    public void paint (Graphics graphics) {
      setBackground (bgColor);
      if (matrix == null) return;
      width = getSize().width;
      height = getSize().height;
      int rows = matrix.length;
      int columns = matrix[0].length; // unter der Annahme, dass alle Zeilen gleich lang sind
      double w = 1.0*width/columns;
      double h = 1.0*height/rows;
      int wInt = (int)w;
      int hInt = (int)h;
      // skalieren
      // alle Matrixelemente ausgeben
      for (int i=0; i<matrix.length; i++)
        for (int j=0; j<matrix[i].length; j++) {
          // Farbe ausrechnen
          double wert = (matrix[i][j]-minValue) / (maxValue-minValue);
          if (wert<0) wert = 0f;
          if (wert >1) wert = 1f;
          graphics.setColor (farbe(wert)); 
          // Rechteck zeichnen
          graphics.fillRect ((int)(w*j),(int)(h*i),wInt,hInt);
        }
    }

    private Color farbe (double wert) {
      int r, g, b;
      // Verlauf von 0=minColor ueber 0.5=midColor bis 1=maxColor
      if (wert < 0.25) {
        r = (int)(minColor.getRed()  *1 + midColor.getRed()  *4*wert);
        g = (int)(minColor.getGreen()*1 + midColor.getGreen()*4*wert);
        b = (int)(minColor.getBlue() *1 + midColor.getBlue() *4*wert);
      }
      else if (wert < 0.5) {
        r = (int)(minColor.getRed()  *(2-4*wert) + midColor.getRed()  *1);
        g = (int)(minColor.getGreen()*(2-4*wert) + midColor.getGreen()*1);
        b = (int)(minColor.getBlue() *(2-4*wert) + midColor.getBlue() *1);
      }
      else if (wert < 0.75) {
        r = (int)(midColor.getRed()  *1 + maxColor.getRed()  *(4*wert-2));
        g = (int)(midColor.getGreen()*1 + maxColor.getGreen()*(4*wert-2));
        b = (int)(midColor.getBlue() *1 + maxColor.getBlue() *(4*wert-2));
      }
      else {
        r = (int)(midColor.getRed()  *(4-4*wert) + maxColor.getRed()  *1);
        g = (int)(midColor.getGreen()*(4-4*wert) + maxColor.getGreen()*1);
        b = (int)(midColor.getBlue() *(4-4*wert) + maxColor.getBlue() *1);
      }
      if (r>255) r=255;
      if (g>255) g=255;
      if (b>255) b=255;
      return new Color (r, g, b);
    }
    
    public void update (Graphics g) {
      //setBackground (Color.black);
      // nicht loeschen, nur repaint() aufrufen
      paint (g);
    }

  }
// Ende der inneren Klasse

}
