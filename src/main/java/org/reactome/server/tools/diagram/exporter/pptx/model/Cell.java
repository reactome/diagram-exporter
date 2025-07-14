package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.*;
import org.reactome.server.tools.diagram.data.layout.Node;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfile;

import java.awt.*;

import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.setShapeStyle;
import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.setTextFrame;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */

@SuppressWarnings("ALL")
public class Cell extends PPTXNode {

    private static final String PROFILE_TYPE = "cell";
    public static final float PADDING = 8;
    protected final int shapeType = ShapeType.RoundCornerRectangle;
    protected byte shapeFillType = FillType.Solid;
    protected byte lineFillType = FillType.Solid;
    protected byte lineStyle = LineStyle.ThinThin;
    protected double lineWidth = 3;

    public Cell(Node node, Adjustment adjustment, boolean flag, boolean selected) {
        super(node, adjustment, flag, selected);
    }

    @Override
    public void render(IShapeCollection shapes, DiagramProfile profile) {
        Stylesheet stylesheet = new Stylesheet(profile, PROFILE_TYPE, shapeFillType, lineFillType, lineStyle);
        Stylesheet nucleusStyle = new Stylesheet(profile, "cell-nucleus", shapeFillType, lineFillType, lineStyle);

        iGroupShape = shapes.addGroupShape();
        iGroupShape.setName("[GROUP] Cell");

        // Shape definitions
        // - Cell
        iAutoShape = iGroupShape.getShapes().addAutoShape(shapeType, x, y, width, height);
        // - Cell Nucleus
        IAutoShape nucleusShape = iGroupShape.getShapes().addAutoShape(shapeType, x + PADDING, y + PADDING, width - 2 * PADDING, height / 2);
        IAdjustValue roundness = nucleusShape.getAdjustments().get_Item(0);
        roundness.setRawValue((long) 1.3 * roundness.getRawValue());
        // - Text Area
        IAutoShape textShape = iGroupShape.getShapes().addAutoShape(shapeType, x + PADDING, y + PADDING + height / 2, width - 2 * PADDING, height / 2 - 2 * PADDING);

        // Styling of shapes
        setShapeStyle(iAutoShape, stylesheet);
        setShapeStyle(nucleusShape, nucleusStyle);
        textShape.getFillFormat().setFillType(FillType.NoFill);
        textShape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);

        setTextFrame(textShape, displayName, new double[]{0, 0, 0, 0}, stylesheet.getTextColor(), 8, true, true, reactomeId, adjustment);

        Color lineColor = Color.BLACK;
        if (selected) {
            lineColor = stylesheet.getSelectionColor();
        }

    }

}
