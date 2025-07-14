package org.reactome.server.tools.diagram.exporter.pptx.util;

import com.aspose.slides.IConnector;
import com.aspose.slides.IShape;
import com.aspose.slides.IShapeCollection;
import com.aspose.slides.ShapeType;
import org.reactome.server.tools.diagram.exporter.pptx.model.*;

import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.setBeginArrowShape;
import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.setShapeStyle;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */

public class SegmentUtil {

    /**
     * Draw segments can connect a shape to an auxiliary shape, a shape to a backbone start or end
     * and an auxiliary shape to another auxiliary shape.
     *
     * @param shapes the collection of shapes present in the slide
     * @param start  start node to be connected
     * @param end    end node to be connected
     */
    public static void drawSegment(IShapeCollection shapes, IShape start, IShape end, Stylesheet stylesheet) {
        IConnector connector = shapes.addConnector(ShapeType.StraightConnector1, start.getX(), start.getY(), 10, 10);
        setShapeStyle(connector, stylesheet);
        connector.setStartShapeConnectedTo(start);
        connector.setEndShapeConnectedTo(end);
        connector.reroute();
    }

    /**
     * Connectors always go from Node to Backbone/Catalyst/ or Node to Auxiliary shape
     * <p>
     * In order to draw segments only please refer {@link #drawSegment(IShapeCollection, IShape, IShape, Stylesheet)}
     *
     * @param shapes           the collection of shapes present in the slide
     * @param pptxNode         instance PPTXNode, used for special cases like Chemicals or Complexes
     * @param start            start point of a connector
     * @param end              end point of a connector
     * @param renderBeginArrow flag to render an arrow at the beginning of a connector
     */
    public static void connect(IShapeCollection shapes, PPTXNode pptxNode, IShape start, IShape end, boolean renderBeginArrow, Stylesheet stylesheet) {
        IConnector connector = shapes.addConnector(ShapeType.StraightConnector1, start.getX(), start.getY(), 10, 10);
        setShapeStyle(connector, stylesheet);

        if (renderBeginArrow) {
            // in this case, our direction is from NODE to backbone or segment,
            // then the arrow should be in the NODE as begin arrow.
            setBeginArrowShape(connector, stylesheet);
        }

        // Some implementations of PPTXNode have an anchorShape shape to ease and beautify where the anchor point is attached
        int anchorPoint;
        if (pptxNode instanceof Complex) {
            start = ((Complex) pptxNode).getAnchorShape();
            anchorPoint = getAnchorPoint(start, end);
        } else if (pptxNode instanceof Chemical) {
            start = ((Chemical) pptxNode).getAnchorShape();
            anchorPoint = getAnchorPoint(start, end);
        } else if (pptxNode instanceof Gene) {
            start = ((Gene) pptxNode).getAnchorShape();
            anchorPoint = 1;
        } else if (pptxNode instanceof EncapsulatedPathway) {
            start = ((EncapsulatedPathway) pptxNode).getAnchorShape();
            anchorPoint = getAnchorPoint(start, end);
        } else if (pptxNode instanceof EncapsulatedNode) {
            start = ((EncapsulatedNode) pptxNode).getAnchorShape();
            anchorPoint = getAnchorPoint(start, end);
        } else {
            anchorPoint = getAnchorPoint(start, end);
        }

        connector.setStartShapeConnectedTo(start);
        connector.setEndShapeConnectedTo(end);
        connector.reroute(); // it takes the shortest possible path between the shapes it connect.
        connector.setStartShapeConnectionSiteIndex(anchorPoint); // after rerouting we set the start based on our method getAnchorPoint
    }

    /**
     * Define the anchor point of a connector based on the position of the node against
     * the position of the backbone or the last auxiliary shape.
     * Our shapes have 4 anchor points, even though the Octagon or the Ellipse have 8 anchor points
     * we are grouping them inside a rectangle specially used for anchor points and to standardise them.
     * p.s The indexes are in different sequence depends on the shape.
     *
     * @return an int where the anchor point index
     * @see Complex#getAnchorShape()
     * @see Chemical#getAnchorShape()
     */
    private static int getAnchorPoint(IShape node, IShape backbone) {
        boolean bottom = (node.getY() + node.getHeight()) <= (int) backbone.getY();
        boolean top = (node.getY()) >= (int) backbone.getY();
        boolean left = (int) backbone.getX() <= node.getX();
        boolean right = (node.getX() + node.getHeight()) <= backbone.getX();

        if (top) return 0;
        if (left) return 1;
        if (bottom) return 2;
        if (right) return 3;

        return 0;
    }
}
