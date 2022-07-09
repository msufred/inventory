package org.gemseeker.app.views.icons;

import javafx.scene.shape.SVGPath;

/**
 *
 * @author gemini1991
 */
public class AddIcon6 extends AbstractSVGIcon {
    
    private static final double ratio = 1.3125;
    
    public AddIcon6() {
        super();
    }
    
    public AddIcon6(double size) {
        super(size * ratio, size);
    }
    
    @Override
    protected SVGPath createIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M15,12c2.2,0,4-1.8,4-4c0-2.2-1.8-4-4-4c-2.2,0-4,1.8-4,"
                + "4C11,10.2,12.8,12,15,12z M6,10V7H4v3H1v2h3v3h2v-3h3v-2H6z "
                + "M15,14c-2.7,0-8,1.3-8,4v2h16v-2C23,15.3,17.7,14,15,14z");
        return path;
    }

    @Override
    protected String getCssStyle() {
        return "material-add6-icon";
    }

}
