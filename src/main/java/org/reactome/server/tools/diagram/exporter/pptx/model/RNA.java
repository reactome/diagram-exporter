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
public class RNA extends PPTXNode {

    private static final String PROFILE_TYPE = "rna";
    protected final int shapeType = ShapeType.Rectangle;
    protected byte shapeFillType = FillType.Solid;
    protected byte lineFillType = FillType.Solid;
    protected byte lineStyle = LineStyle.Single;

    public RNA(Node node, Adjustment adjustment, boolean flag, boolean select) {
        super(node, adjustment, flag, select);
    }

    @Override
    public void render(IShapeCollection shapes, DiagramProfile profile) {
        Stylesheet stylesheet = new Stylesheet(profile, PROFILE_TYPE, shapeFillType, lineFillType, lineStyle);
        render(shapes, shapeType, stylesheet);

        // TODO Render a RNA shape
    }
}
