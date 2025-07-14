package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.*;
import org.reactome.server.tools.diagram.data.layout.Node;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfile;

import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.*;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */

@SuppressWarnings("ALL")
public class ComplexDrug extends Complex {

    private static final String PROFILE_TYPE = "complexdrug";
    private final int shapeType = ShapeType.Octagon;
    private byte shapeFillType = FillType.Solid;
    private byte lineFillType = FillType.Solid;
    private byte lineStyle = LineStyle.Single;

    // Shape that the connector will be connected. This is a simple rectangle with 4 anchor points only
    private IAutoShape anchorShape;

    public ComplexDrug(Node node, Adjustment adjustment, boolean flag, boolean select) {
        super(node, adjustment, flag, select);
    }

    public IAutoShape getAnchorShape() {
        return anchorShape;
    }

    @Override
    public void render(IShapeCollection shapes, DiagramProfile profile) {
        Stylesheet stylesheet = new Stylesheet(profile, PROFILE_TYPE, shapeFillType, lineFillType, lineStyle,8);

        render(shapes, shapeType, stylesheet);

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

        anchorShape = iGroupShape.getShapes().addAutoShape(ShapeType.Rectangle, iAutoShape.getX() - 2, iAutoShape.getY() - 2, iAutoShape.getWidth() + 2, iAutoShape.getHeight() + 2);
        anchorShape.setName("Auxiliary Shape");
        anchorShape.getFillFormat().setFillType(FillType.NoFill);
        anchorShape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);

        IAutoShape rx = iGroupShape.getShapes().addAutoShape(
                ShapeType.Rectangle,
                (iAutoShape.getX()+iAutoShape.getWidth())-14,
                (iAutoShape.getY()+iAutoShape.getHeight())-8,
                8,
                6
        );
        setDrugShapeStyle(rx, stylesheet, false);

        rx.setName("Rx");
        setTextFrame(rx, "Rx", new double[]{0, 0, 0, 0}, stylesheet.getTextColor(), 6, true, false, reactomeId, adjustment);

        if (selected) {
            setSelectedStyle(rx, stylesheet);
        }

        rx.getAutoShapeLock().setSelectLocked(true);
    }
}
