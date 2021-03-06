package org.gemseeker.app.views.icons;

import javafx.scene.shape.SVGPath;

public class RefreshIcon extends AbstractSVGIcon {

    public RefreshIcon() {
        super();
    }

    public RefreshIcon(double size) {
        super(size * 0.75, size);
    }

    @Override
    protected SVGPath createIcon() {
        SVGPath path = new SVGPath();
        path.setContent("M12,4V1L8,5l4,4V6c3.3,0,6,2.7,6,6c0,1-0.3,2-0.7,2.8l1.5,1"
                + ".5C19.5,15,20,13.6,20,12C20,7.6,16.4,4,12,4z M12,18c-3.3,0-6-2."
                + "7-6-6c0-1,0.3-2,0.7-2.8L5.2,7.7C4.5,9,4,10.4,4,12c0,4.4,3.6,8,8"
                + ",8v3l4-4l-4-4V18z");
        return path;
    }

    @Override
    protected String getCssStyle() {
        return "material-refresh-icon";
    }

}