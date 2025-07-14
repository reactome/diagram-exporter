package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.IAutoShape;
import com.aspose.slides.IGroupShape;
import com.aspose.slides.IShapeCollection;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.Stoichiometry;

import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.*;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */
public class PPTXStoichiometry {

    private static final String PROFILE_TYPE = "stoichiometry";
    private IGroupShape iGroupShape;
    private IAutoShape iAutoShape;
    private IAutoShape hiddenCenterShape; // hidden behind the stoichiometry shape

    private Integer value;
    private Shape shape;
    private Adjustment adjustment;

    public PPTXStoichiometry(Stoichiometry stoichiometry, Adjustment adjustment) {
        this.shape = stoichiometry.getShape();
        this.value = stoichiometry.getValue();
        this.adjustment = adjustment;
    }

    public PPTXStoichiometry render(IShapeCollection shapes, Stylesheet stylesheet) {
        iGroupShape = shapes.addGroupShape();
        float xCenter = (shape.getA().getX().floatValue() + shape.getB().getX().floatValue()) / 2;
        float yCenter = (shape.getA().getY().floatValue() + shape.getB().getY().floatValue()) / 2;
        hiddenCenterShape = renderAuxiliaryShape(iGroupShape, xCenter, yCenter, adjustment);
        iAutoShape = renderShape(iGroupShape, shape, stylesheet, adjustment);
        setTextFrame(iAutoShape, value.toString(), new double[]{0, 0, 0, 0}, stylesheet.getTextColor(), 8, true, false, null, adjustment);
        iGroupShape.getGroupShapeLock().setSizeLocked(true);
        return this;
    }

    public IAutoShape getHiddenCenterShape() {
        return hiddenCenterShape;
    }

    public IGroupShape getiGroupShape() {
        return iGroupShape;
    }
}
