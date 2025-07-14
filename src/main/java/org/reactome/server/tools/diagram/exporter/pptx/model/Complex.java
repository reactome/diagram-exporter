package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.*;
import org.reactome.server.tools.diagram.data.layout.Node;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfile;

import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.setSelectedStyle;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */

@SuppressWarnings("ALL")
public class Complex extends PPTXNode {

    private static final String PROFILE_TYPE = "complex";
    private final int shapeType = ShapeType.Octagon;
    byte shapeFillType = FillType.Solid;
    byte lineFillType = FillType.Solid;
    byte lineStyle = LineStyle.Single;

    // Shape that the connector will be connected. This is a simple rectangle with 4 anchor points only
    private IAutoShape anchorShape;

    public Complex(Node node, Adjustment adjustment, boolean flag, boolean selected) {
        super(node, adjustment, flag, selected);
    }

    public IAutoShape getAnchorShape() {
        return anchorShape;
    }

    @Override
    public void render(IShapeCollection shapes, DiagramProfile profile) {
        Stylesheet stylesheet = new Stylesheet(profile, PROFILE_TYPE, shapeFillType, lineFillType, lineStyle);

        render(shapes, shapeType, stylesheet);

        anchorShape = iGroupShape.getShapes().addAutoShape(ShapeType.Rectangle, iAutoShape.getX() - 2, iAutoShape.getY() - 2, iAutoShape.getWidth() + 2, iAutoShape.getHeight() + 2);
        anchorShape.setName("Auxiliary Shape");
        anchorShape.getFillFormat().setFillType(FillType.NoFill);
        anchorShape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);

        IAdjustValueCollection adjustments = iAutoShape.getAdjustments();
        IAdjustValue adjustValue = null;
        if (adjustments != null && adjustments.size() > 0) {
            adjustValue = iAutoShape.getAdjustments().get_Item(0);
        }

        if (adjustValue != null) {
            // 0 to 50000 - Where 0 correspond to a value that will reduce the distance between two points such that a rectangle is formed
            adjustValue.setRawValue(14822);

            //0 to 0.833333
            adjustValue.setAngleValue(0.2470333f);
        }

        if (selected) {
            setSelectedStyle(iAutoShape, stylesheet);
        }
    }
}
