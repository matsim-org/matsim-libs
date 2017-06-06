// code by varunpant
package playground.clruch.gheat;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import playground.clruch.gheat.graphics.BlendComposite;
import playground.clruch.gheat.graphics.ColorScheme;
import playground.clruch.gheat.graphics.DotImage;
import playground.clruch.gheat.graphics.GammaCorrection;

public class Tile {
    private Tile() {
    }

    public static BufferedImage Generate(ColorScheme colorScheme, DotImage dot, //
            int zoom, int tileX, int tileY, DataPoint[] points) throws Exception {
        int expandedWidth;
        int expandedHeight;
        int x1;
        int x2;
        int y1;
        int y2;
        // Translate tile to pixel coords.
        x1 = tileX * HeatMap.SIZE;
        x2 = x1 + 255;
        y1 = tileY * HeatMap.SIZE;
        y2 = y1 + 255;
        int extraPad = dot.getWidth() * 2;
        // Expand bounds by one dot width.
        x1 = x1 - extraPad;
        x2 = x2 + extraPad;
        y1 = y1 - extraPad;
        y2 = y2 + extraPad;
        expandedWidth = x2 - x1;
        expandedHeight = y2 - y1;
        BufferedImage tile;
        if (points.length == 0) {
            tile = GetEmptyTile(colorScheme);
        } else {
            tile = GetBlankImage(expandedHeight, expandedWidth);
            tile = AddPoints(tile, dot, points);
            tile = Trim(tile, dot.bufferedImage);
            tile = Colorize(tile, colorScheme.bufferedImage);
        }
        return tile;
    }

    /// Takes the gray scale and applies the color scheme to it.
    public static BufferedImage Colorize(BufferedImage tile, BufferedImage palette) {
        Color tilePixelColor;
        // Color colorSchemePixel;
        for (int x = 0; x < tile.getWidth(); x++) {
            for (int y = 0; y < tile.getHeight(); y++) {
                // Get color for this intensity
                tilePixelColor = new Color(tile.getRGB(x, y));
                // Get the color of the scheme from the intensity on the tile
                // Only need to get one color in the tile, because it is grayscale, so each color will have the same intensity
                int rgba = palette.getRGB(0, tilePixelColor.getRed());
                // colorSchemePixel = new Color();
                // zoomOpacity = (int) ((((double) zoomOpacity / 255.0f) * ((double) colorSchemePixel.getAlpha() / 255.0f)) * 255f);
                Color color = new Color(rgba, true);
                tile.setRGB(x, y, color.getRGB());
            }
        }
        return tile;
    }

    /// <summary>
    /// Trim the larger tile to the correct size
    /// </summary>
    /// <param name="tile"></param>
    /// <returns></returns>
    public static BufferedImage Trim(BufferedImage tile, BufferedImage dot) {
        BufferedImage croppedTile = new BufferedImage(HeatMap.SIZE, HeatMap.SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics g = croppedTile.createGraphics();
        int adjPad = dot.getWidth() + (dot.getWidth() / 2);
        g.drawImage(tile, // Source Image
                0, 0, HeatMap.SIZE, HeatMap.SIZE, adjPad, // source x, adjusted for padded amount
                adjPad, // source y, adjusted for padded amount
                HeatMap.SIZE + adjPad, // source width
                HeatMap.SIZE + adjPad, // source height
                null);
        g.dispose();
        return croppedTile;
    }

    /// <summary>
    /// Add all of the points to the tile
    /// </summary>
    /// <param name="tile"></param>
    /// <param name="points"></param>
    /// <returns></returns>
    public static BufferedImage AddPoints(BufferedImage tile, DotImage dot, DataPoint[] points) {
        Graphics2D g = tile.createGraphics();
        g.setComposite(BlendComposite.Multiply);
        for (int i = 0; i < points.length; i++) {
            // double weight = points[i].getWeight();
            // BufferedImage src = dot; // weight != 0 ? ApplyWeightToImage(dot, weight) : dot;
            g.drawImage(dot.bufferedImageRGB, (int) (points[i].getX() + dot.getWidth()), (int) (points[i].getY() + dot.getWidth()), null);
        }
        g.dispose();
        return tile;
    }

    /// <summary>
    /// Change the intensity of the image
    /// </summary>
    /// <param name="image">Dot image</param>
    /// <param name="weight">Weight to apply</param>
    /// <returns></returns>
    private static BufferedImage ApplyWeightToImage(BufferedImage image, double weight) {
        Graphics2D graphic;
        double tempWeight;
        BufferedImage tempImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        graphic = tempImage.createGraphics();
        // I want to make the color more intense (White/bright)
        tempWeight = weight;
        AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f);
        graphic.setComposite(composite);
        graphic.drawImage(image, // Source Image
                0, 0, image.getWidth(), image.getHeight(), 0, 0, image.getWidth(), image.getHeight(), // source height
                null);
        // New dot with a different intensity
        return GammaCorrection.gammaCorrection(tempImage, (tempWeight == 0 ? .1f : (tempWeight * 5)));
    }

    /// <summary>
    /// Gets a blank image / canvas
    /// </summary>
    /// <returns></returns>
    public static BufferedImage GetBlankImage(int height, int width) {
        BufferedImage newImage;
        Graphics2D g;
        // Create a blank tile that is 32 bit and has an alpha
        newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g = newImage.createGraphics();
        // Background must be white so the dots can blend
        g.setBackground(Color.WHITE);
        g.fillRect(0, 0, height, width);
        g.dispose();
        return newImage;
    }

    /* Empty tile with no points on it. */
    public static BufferedImage GetEmptyTile(ColorScheme colorScheme) {
        // If we have already created the empty tile then return it
        if (Cache.hasEmptyTile(colorScheme))
            return Cache.getEmptyTile(colorScheme);
        // System.out.println("create empty tile: " + colorScheme);
        // Create a blank tile that is 32 bit and has an alpha
        BufferedImage tile = new BufferedImage(HeatMap.SIZE, HeatMap.SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphic = tile.createGraphics();
        // Get the first pixel of the color scheme, on the dark side
        BufferedImage palette = colorScheme.bufferedImage;
        int rgba = palette.getRGB(0, palette.getHeight() - 1);
        graphic.setColor(new Color(rgba, true));
        graphic.fillRect(0, 0, HeatMap.SIZE, HeatMap.SIZE);
        // graphic.setColor(Color.BLACK);
        // graphic.drawString("[empty]", 10, 10);
        graphic.dispose();
        // Save the newly created empty tile
        // There is a empty tile for each scheme and zoom level
        // Double check it does not already exists
        if (!Cache.hasEmptyTile(colorScheme))
            Cache.putEmptyTile(colorScheme, tile);
        return tile;
    }
}
