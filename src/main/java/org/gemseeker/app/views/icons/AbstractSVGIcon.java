package org.gemseeker.app.views.icons;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public abstract class AbstractSVGIcon extends Region {
    protected double prefWidth = 18;
    protected double prefHeight = 18;

    public AbstractSVGIcon() {
        super();
        initIcon();
    }

    public AbstractSVGIcon(double width, double height) {
        super();
        this.prefWidth = width;
        this.prefHeight = height;
        initIcon();
    }

    public AbstractSVGIcon(double size) {
        this.prefWidth = size;
        this.prefHeight = size;
        initIcon();
    }

    private void initIcon() {
        setPrefSize(prefWidth, prefHeight);
        setMaxSize(prefWidth, prefHeight);
        getStyleClass().add("material-icon");
        getStyleClass().add(getCssStyle());
        setShape(createIcon());
        setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    protected abstract SVGPath createIcon();
    protected abstract String getCssStyle();
}
