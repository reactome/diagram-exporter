package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.FillType;
import com.aspose.slides.IShapeCollection;
import com.aspose.slides.LineStyle;
import com.aspose.slides.ShapeType;
import org.reactome.server.tools.diagram.data.layout.Node;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfile;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */

@SuppressWarnings("ALL")
public class EntitySet extends PPTXNode {

    private static final String PROFILE_TYPE = "entityset";
    protected final int shapeType = ShapeType.RoundCornerRectangle;
    protected byte shapeFillType = FillType.Solid;
    protected byte lineFillType = FillType.Solid;
    protected byte lineStyle = LineStyle.ThinThin;
    protected double lineWidth = 3;

    public EntitySet(Node node, Adjustment adjustment, boolean flag, boolean select) {
        super(node, adjustment, flag, select);
    }

    @Override
    public void render(IShapeCollection shapes, DiagramProfile profile) {
        Stylesheet stylesheet = new Stylesheet(profile, PROFILE_TYPE, shapeFillType, lineFillType, lineStyle);
        stylesheet.setLineWidth(lineWidth);
        render(shapes, shapeType, stylesheet);
    }
}
