package playground.clruch.gheat;

public class Size {
    private double height;
    private double width;

    /* Height */
    public double getHeight() {
        return height;
    }

    /* Width */
    public double getWidth() {
        return width;
    }

    /* Constructor */
    public Size(double width, double height) {
        this.width = width;
        this.height = height;
    }
}