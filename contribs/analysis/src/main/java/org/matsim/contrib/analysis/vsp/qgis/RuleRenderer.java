package org.matsim.contrib.analysis.vsp.qgis;

/**
 *
 * @author gthunig on 17.05.2017
 *
 */
public abstract class RuleRenderer extends QGisRenderer {

    private String renderingAttribute;
    private boolean useHeader;
    private String fileHeader;

    public RuleRenderer(String header, QGisLayer layer) {

        super(QGisConstants.renderingType.RuleRenderer, layer);
        this.useHeader = header != null;
        this.fileHeader = header;

    }

    public abstract Rule[] getRules();

    public String getRenderingAttribute(){

        return this.renderingAttribute;

    }

    public void setRenderingAttribute(String attr){

        if(this.useHeader){

            if(this.fileHeader.contains(attr)){

                this.renderingAttribute = attr;

            } else{

                throw new RuntimeException("Rendering attribute " + attr + " does not exist in header!");

            }

        } else{

            throw new RuntimeException("The input file for this renderer has no header. Use method \"setRenderingAttribute(int columnIndex)\" instead!");

        }

    }

    public void setRenderingAttribute(int columnIndex){

        if(!this.useHeader){

            this.renderingAttribute = "field_" + Integer.toString(columnIndex);

        } else{

            throw new RuntimeException("The input file for this renderer has a header. Use method \"setRenderingAttribute(String attr)\" instead!");

        }

    }

}
