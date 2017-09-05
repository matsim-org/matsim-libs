// code by jph
package playground.clib.util.gui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;

public class DisplayHelper {
  private Rectangle myScreen = new Rectangle();

  public DisplayHelper() {
    GraphicsEnvironment myGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (GraphicsDevice myGraphicsDevice : myGraphicsEnvironment.getScreenDevices())
      for (GraphicsConfiguration myGraphicsConfiguration : myGraphicsDevice.getConfigurations())
        myScreen = myScreen.union(myGraphicsConfiguration.getBounds());
  }

  public Rectangle allVisible(int x, int y, int width, int height) {
    x = Math.max(0, Math.min(x, myScreen.width - width));
    y = Math.max(0, Math.min(y, myScreen.height - height));
    return new Rectangle(x, y, width, height);
  }

  public Rectangle allVisible(Rectangle myRectangle) {
    return allVisible(myRectangle.x, myRectangle.y, myRectangle.width, myRectangle.height);
  }

  public Rectangle getScreenRectangle() {
    return myScreen;
  }

  @Override
  public String toString() {
    return "Display point=(" + myScreen.x + ", " + myScreen.y + ") dimension=(" + myScreen.width + ", " + myScreen.height + ")";
  }

  public static Point getMouseLocation() {
    try {
      // can test with GraphicsEnvironment.isHeadless()
      return MouseInfo.getPointerInfo().getLocation();
    } catch (Exception myException) {
      myException.printStackTrace();
    }
    return new Point();
  }
}
