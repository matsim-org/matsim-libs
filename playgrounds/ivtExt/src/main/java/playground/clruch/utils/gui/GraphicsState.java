package playground.clruch.utils.gui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class GraphicsState {

    final Graphics2D graphics;
    final Object antiAliasing;
    final Object interpolation;
    final Object rendering;

    public GraphicsState(Graphics2D graphics) {
        this.graphics = graphics;
        antiAliasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        interpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        rendering = graphics.getRenderingHint(RenderingHints.KEY_RENDERING);
    }

    public void restore() {
        if (antiAliasing != null)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAliasing);
        if (interpolation != null)
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        if (rendering != null)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, rendering);
    }

}
