package org.gemseeker.app.views.icons;

import javafx.scene.shape.SVGPath;

/**
 *
 * @author gemini1991
 */
public class PrintIcon extends AbstractSVGIcon {
    
    public PrintIcon() {
        super();
    }
    
    public PrintIcon(double size) {
        super(size, size * 0.875);
    }
    
    @Override
    protected SVGPath createIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M19,8H5c-1.7,0-3,1.3-3,3v6h4v4h12v-4h4v-6C22,9.3,20.7,8,"
                + "19,8z M16,19H8v-5h8V19z M19,12c-0.6,0-1-0.4-1-1s0.4-1,1-1c0.6,"
                + "0,1,0.4,1,1S19.6,12,19,12z M18,3H6v4h12V3z");
        return path;
    }

    @Override
    protected String getCssStyle() {
        return "material-print-icon";
    }

}
