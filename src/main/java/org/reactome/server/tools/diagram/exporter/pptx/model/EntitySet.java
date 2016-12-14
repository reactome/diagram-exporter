package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.FillType;
import com.aspose.slides.IShapeCollection;
import com.aspose.slides.LineStyle;
import com.aspose.slides.ShapeType;
import org.reactome.server.tools.diagram.data.layout.Node;
import org.reactome.server.tools.diagram.exporter.common.profiles.model.DiagramProfile;

/**
 * @author Guilherme S Viteri <gviteri@ebi.ac.uk>
 */

@SuppressWarnings("ALL")
public class EntitySet extends PPTXNode {

    private final int shapeType = ShapeType.RoundCornerRectangle;
    private byte shapeFillType = FillType.Solid;
    private byte lineFillType = FillType.Solid;
    private byte lineStyle = LineStyle.ThinThin;
    private double lineWidth = 3;

    public EntitySet(Node node) {
        super(node);
    }

    @Override
    public void render(IShapeCollection shapes, DiagramProfile profile) {
        Stylesheet stylesheet = new Stylesheet(profile.getEntityset(), shapeFillType, lineFillType, lineStyle);
        stylesheet.setLineWidth(lineWidth);
        render(shapes, shapeType, stylesheet);
    }
}