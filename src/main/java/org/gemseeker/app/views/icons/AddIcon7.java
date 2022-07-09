package org.gemseeker.app.views.icons;

import javafx.scene.shape.SVGPath;

/**
 *
 * @author gemini1991
 */
public class AddIcon7 extends AbstractSVGIcon {
    
    public AddIcon7() {
        super();
    }
    
    public AddIcon7(double size) {
        super(size);
    }
    
    @Override
    protected SVGPath createIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6V13z");
        return path;
    }

    @Override
    protected String getCssStyle() {
        return "material-add7-icon";
    }

}
