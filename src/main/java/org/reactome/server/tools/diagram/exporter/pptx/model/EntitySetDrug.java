package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.FillType;
import com.aspose.slides.IAutoShape;
import com.aspose.slides.IShapeCollection;
import com.aspose.slides.ShapeType;
import org.reactome.server.tools.diagram.data.layout.Node;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfile;

import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.*;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */

@SuppressWarnings("ALL")
public class EntitySetDrug extends EntitySet {

    private static final String PROFILE_TYPE = "entitysetdrug";

    // Shape that the connector will be connected. This is a simple rectangle with 4 anchor points only
    private IAutoShape anchorShape;

    public EntitySetDrug(Node node, Adjustment adjustment, boolean flag, boolean select) {
        super(node, adjustment, flag, select);
    }

    public IAutoShape getAnchorShape() {
        return anchorShape;
    }

    @Override
    public void render(IShapeCollection shapes, DiagramProfile profile) {
        Stylesheet stylesheet = new Stylesheet(profile, PROFILE_TYPE, shapeFillType, lineFillType, lineStyle, 8);

        render(shapes, shapeType, stylesheet);

        anchorShape = iGroupShape.getShapes().addAutoShape(ShapeType.Rectangle, iAutoShape.getX(), iAutoShape.getY(), iAutoShape.getWidth(), iAutoShape.getHeight());
        anchorShape.setName("Auxiliary Shape");
        anchorShape.getFillFormat().setFillType(FillType.NoFill);
        anchorShape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);

        IAutoShape rx = iGroupShape.getShapes().addAutoShape(
                ShapeType.Rectangle,
                (iAutoShape.getX()+iAutoShape.getWidth())-12,
                (iAutoShape.getY()+iAutoShape.getHeight())-9,
                10,
                6
        );
        setDrugShapeStyle(rx, stylesheet, false);

        rx.setName("Rx");
        setTextFrame(rx, "Rx", new double[]{0, 0, 0, 0}, stylesheet.getTextColor(), 6, true, false, reactomeId, adjustment);

        if (selected) {
            setSelectedStyle(rx, stylesheet);
        }

        // block the nodeattachments to be selected :)
        rx.getAutoShapeLock().setSelectLocked(true);
    }
}
