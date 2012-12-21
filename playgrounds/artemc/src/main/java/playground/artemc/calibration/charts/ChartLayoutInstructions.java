package playground.artemc.calibration.charts;

/**
 * ChartLayoutInstructions are used to specify how charts should be
 * layed out on screen and in print format.  
 *
 */
public class ChartLayoutInstructions {

    int rows;
    int columns;

    /**
     * Constructor
     * @param rows number of rows in the display/print grid
     * @param columns number of columns in the display/print grid
     */
    public ChartLayoutInstructions(int rows, int columns, boolean allowSwap){
        this.rows = Math.abs(rows);
        this.columns = Math.abs(columns);
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = Math.abs(rows);
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = Math.abs(columns);
    }
}