/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.singapore.calibration.charts;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.util.ArrayList;
import java.util.List;


/**
 * The CustomChartPanel is used to display/print multiple JFreeCharts
 * Users should only interact with this class with the methods defined
 * below in order to get the proper functionality.  Using
 * inherited methods may produce unwanted display/print behavior if you
 * add components other than JFreeCharts.
 *
 */
public class CustomChartPanel extends JPanel implements Printable{

    List<JFreeChart> charts = new ArrayList<JFreeChart>();
    List<ChartPanel> panels = new ArrayList<ChartPanel>();
    ChartLayoutInstructions layoutInstructions;

    public CustomChartPanel(){
        super();
    }

    public CustomChartPanel(JFreeChart chart){
        super();
        charts.add(chart);
    }

    /**
     * Creates a CustomChartPanel which displays 1 or more charts in a grid-like fashion
     * described by the layoutInstructions you pass in.  Note that if you pass in more
     * charts than there are columns specified in the ChartLayoutInstructions then excess
     * charts will not be displayed or printed.
     * @param charts
     * @param layoutInstructions
     */
    public CustomChartPanel(List<JFreeChart> charts, ChartLayoutInstructions layoutInstructions){
        super();
        this.layoutInstructions = layoutInstructions;
        for(JFreeChart chart : charts){
            this.charts.add(chart);
        }
        createUIComponents();
    }

    protected void createUIComponents(){
        int size = Math.min(layoutInstructions.getColumns() * layoutInstructions.getRows(), charts.size());
        this.setLayout(new GridLayout(layoutInstructions.getRows(), layoutInstructions.getColumns()));


        for(int i = 0; i < size; i++ ){
            System.err.println("Adding chart");
            ChartPanel chartPanel = new ChartPanel(charts.get(i));
            chartPanel.setMaximumDrawHeight(20000);
            chartPanel.setMinimumDrawHeight(0);
            chartPanel.setMaximumDrawWidth(20000);
            chartPanel.setMinimumDrawWidth(0);
            chartPanel.setPopupMenu(null);
            panels.add(chartPanel);
            this.add(chartPanel);
        }
    }

    public void createPrintJob(){
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        PageFormat pf2 = job.pageDialog(pf);
        if (pf2 != pf) {
            job.setPrintable(this, pf2);
            if (job.printDialog()) {
                try {
                    job.print();
                }
                catch (PrinterException e) {
                    JOptionPane.showMessageDialog(this, e);
                }
            }
        }
    }

    @Override
    public int print(Graphics g, PageFormat pf, int pageIndex)
            throws PrinterException {
        System.err.println("PRINTING");
        //Divide the current page format into sections based
        //on the layout instructions received in the constructor
        //a new pagelayout is created for each cell in the grid
        //that will then be passed along to the print method of
        //each chart panel.


        if(pageIndex != 0){
            return NO_SUCH_PAGE;
        }

        List<PageFormat> pageFormats = new ArrayList<PageFormat>();

        //setup all the page formats needed for the grid cells.
        double x = pf.getImageableX();
        double y = pf.getImageableY();
        double cellWidth = pf.getImageableWidth() / layoutInstructions.getColumns();
        double cellHeight = pf.getImageableHeight() / layoutInstructions.getRows();

        for(int i=1; i <= layoutInstructions.getRows(); i++){
            double rowOffset = (i-1)*cellHeight + y;
            for(int j=1; j <= layoutInstructions.getColumns(); j++){
                PageFormat format = new PageFormat();
                Paper paper = new Paper();
                double columnOffset = (j-1)*cellWidth + x;
                paper.setImageableArea(columnOffset, rowOffset, cellWidth, cellHeight);
                format.setPaper(paper);
                pageFormats.add(format);
            }
        }

        //have each chartpanel print on the graphics context using its
        //particular PageFormat
        int size = Math.min(pageFormats.size(), panels.size());
        for(int i = 0; i < size; i++ ){
            panels.get(i).print(g, pageFormats.get(i), pageIndex);

        }

        return PAGE_EXISTS;
    }
}