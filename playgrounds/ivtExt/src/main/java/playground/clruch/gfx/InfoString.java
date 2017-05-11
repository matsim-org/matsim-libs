package playground.clruch.gfx;

import java.awt.Color;

/**
 * the {@link InfoString} is displayed in viewer
 */
public class InfoString {
    public final String message;
    public Color color = Color.BLACK;

    public InfoString(String message) {
        this.message = message;
    }

}
