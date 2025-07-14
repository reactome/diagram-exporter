package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.*;
import org.reactome.server.tools.diagram.data.layout.Node;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfile;

import java.awt.*;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */
@SuppressWarnings("all")
public class EncapsulatedPathway extends PPTXNode {

    private static final String PROFILE_TYPE = "processnode";
    private final int shapeType = ShapeType.Rectangle;
    private IAutoShape anchorShape;
    private byte shapeFillType = FillType.Solid;
    private byte lineFillType = FillType.Solid;
    private byte lineStyle = LineStyle.Single;

    public EncapsulatedPathway(Node node, Adjustment adjustment, boolean flag, boolean select) {
        super(node, adjustment, flag, select);
    }

    @Override
    public void render(IShapeCollection shapes, DiagramProfile profile) {
        // Rendering the EnsapsulatedPathway slightly different from the PWB
        // Line color is coming from the profile, but the filling of EncapsulatedPathway is always white.
        Stylesheet stylesheet = new Stylesheet(profile, PROFILE_TYPE, shapeFillType, lineFillType, lineStyle);
        // LineWidth is not present in the profile, so create a thick line (width=10)
        stylesheet.setLineWidth(10);
        // Line color is the fill color provided by the profile.
        stylesheet.setLineColor(stylesheet.getFillColor());
        // FillColor in the profile is the line fill color, so we are hardcoding color white
        stylesheet.setFillColor(Color.WHITE);

        if (isFadeOut) {
            stylesheet.setLineColor(stylesheet.getFadeOutFill());
            stylesheet.setFillColor(Color.WHITE);
            stylesheet.setTextColor(stylesheet.getTextColor());
            isFadeOut = false;
        }

        render(shapes, shapeType, stylesheet);

        anchorShape = iGroupShape.getShapes().addAutoShape(ShapeType.Rectangle, iAutoShape.getX() - 5, iAutoShape.getY() - 5, iAutoShape.getWidth() + 12, iAutoShape.getHeight() + 12);
        anchorShape.setName("Auxiliary Shape");
        anchorShape.getFillFormat().setFillType(FillType.NoFill);
        anchorShape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);

        iGroupShape.getShapes().reorder(iGroupShape.getShapes().size() - 1, iAutoShape);
    }

    public IAutoShape getAnchorShape() {
        return anchorShape;
    }
}
