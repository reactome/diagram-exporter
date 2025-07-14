package org.reactome.server.tools.diagram.exporter.pptx.util;

import com.aspose.slides.*;
import org.reactome.server.tools.diagram.data.layout.Connector;
import org.reactome.server.tools.diagram.data.layout.*;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.impl.NodePropertiesFactory;
import org.reactome.server.tools.diagram.exporter.pptx.model.Adjustment;
import org.reactome.server.tools.diagram.exporter.pptx.model.PPTXNode;
import org.reactome.server.tools.diagram.exporter.pptx.model.PPTXStoichiometry;
import org.reactome.server.tools.diagram.exporter.pptx.model.Stylesheet;

import java.awt.Color;
import java.util.Collection;
import java.util.Map;

import static org.reactome.server.tools.diagram.exporter.pptx.util.SegmentUtil.drawSegment;

/**
 * This class is concentrating all rendering and Styling shapes.
 * Do not style
 *
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */
public class PPTXShape {

    public static IAutoShape renderShape(IGroupShape group, Shape rctShape, Stylesheet stylesheet, Adjustment adjustment) {
        switch (rctShape.getType()) {
            case "CIRCLE":
                NodeProperties npC = NodePropertiesFactory.transform(NodePropertiesFactory.get(rctShape.getC().getX().floatValue(), rctShape.getC().getY().floatValue(), rctShape.getR().floatValue(), rctShape.getR().floatValue()), adjustment.getFactor(), adjustment.getPanning());
                float correction = npC.getWidth().floatValue();
                IAutoShape circle = group.getShapes().addAutoShape(
                        ShapeType.Ellipse,
                        npC.getX().floatValue() - correction,
                        npC.getY().floatValue() - correction,
                        npC.getWidth().floatValue() + correction,
                        npC.getHeight().floatValue() + correction
                );
                setShapeStyle(circle, new Stylesheet().customStyle(1, LineStyle.NotDefined, FillType.Solid, stylesheet.getLineColor(), FillType.Solid, stylesheet.getFillColor(), stylesheet.getLineDashStyle()));
                circle.getAutoShapeLock().setSizeLocked(true);
                return circle;
            case "DOUBLE_CIRCLE":
                NodeProperties npDC = NodePropertiesFactory.transform(NodePropertiesFactory.get(rctShape.getC().getX().floatValue(), rctShape.getC().getY().floatValue(), rctShape.getR().floatValue(), rctShape.getR().floatValue()), adjustment.getFactor(), adjustment.getPanning());
                correction = npDC.getWidth().floatValue();
                IAutoShape dCircle = group.getShapes().addAutoShape(
                        ShapeType.Ellipse,
                        npDC.getX().floatValue() - correction,
                        npDC.getY().floatValue() - correction,
                        npDC.getWidth().floatValue() + correction,
                        npDC.getHeight().floatValue() + correction
                );
                setShapeStyle(dCircle, new Stylesheet().customStyle(2.75, LineStyle.ThinThin, FillType.Solid, stylesheet.getLineColor(), FillType.Solid, Color.WHITE, stylesheet.getLineDashStyle()));
                dCircle.getAutoShapeLock().setSizeLocked(true);
                return dCircle;
            case "BOX":
                float w = rctShape.getB().getX().floatValue() - rctShape.getA().getX().floatValue();
                float h = rctShape.getB().getY().floatValue() - rctShape.getA().getY().floatValue();
                NodeProperties npBOX = NodePropertiesFactory.transform(NodePropertiesFactory.get(rctShape.getA().getX().floatValue(), rctShape.getA().getY().floatValue(), w, h), adjustment.getFactor(), adjustment.getPanning());
                IAutoShape box = group.getShapes().addAutoShape(
                        ShapeType.Rectangle,
                        npBOX.getX().floatValue(),
                        npBOX.getY().floatValue(),
                        npBOX.getWidth().floatValue(),
                        npBOX.getHeight().floatValue());
                setShapeStyle(box, new Stylesheet().customStyle(1, LineStyle.NotDefined, FillType.Solid, stylesheet.getLineColor(), FillType.Solid, Color.WHITE, stylesheet.getLineDashStyle()));
                box.getAutoShapeLock().setSizeLocked(true);
                if (rctShape.getS() != null) {
                    setTextFrame(box, rctShape.getS(), new double[]{0, 0, 0, 0}, stylesheet.getTextColor(), 6, true, false, null, adjustment);
                }
                return box;
            default:
                throw new RuntimeException(rctShape.getType() + " hasn't been recognised.");
        }
    }

    /**
     * Retrieve IAutoShape used as a support for drawing connectors, which are meant for connecting shapes only.
     *
     * @return formatted IAutoShape - No line and No fill as part of group shape
     */
    public static IAutoShape renderAuxiliaryShape(IGroupShape group, Coordinate edgeCoordinate, Stylesheet stylesheet, Adjustment adjustment) {
        return renderAuxiliaryShape(group.getShapes(), edgeCoordinate, stylesheet, adjustment);
    }

    /**
     * Retrieve IAutoShape used as a support for drawing connectors, which are meant for connecting shapes only.
     *
     * @param shapes collection of shapes present in the Slides
     * @return rendered IAutoShape as it is
     */
    public static IAutoShape renderAuxiliaryShape(IShapeCollection shapes, Coordinate edgeCoordinate, Stylesheet stylesheet, Adjustment adjustment) {
        NodeProperties np = NodePropertiesFactory.transform(NodePropertiesFactory.get(edgeCoordinate.getX().floatValue(), edgeCoordinate.getY().floatValue(), 2, 2), adjustment.getFactor(), adjustment.getPanning());
        IAutoShape auxShape = shapes.addAutoShape(ShapeType.Ellipse, np.getX().floatValue(), np.getY().floatValue(), np.getWidth().floatValue(), np.getHeight().floatValue());
        auxShape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        auxShape.getFillFormat().setFillType(FillType.Solid);
        auxShape.getFillFormat().getSolidFillColor().setColor(stylesheet.getFillColor());
        auxShape.getAutoShapeLock().setSizeLocked(true);
        return auxShape;
    }

    /**
     * Retrieve IAutoShape used as a support for drawing connectors, which are meant for connecting shapes only.
     *
     * @return rendered IAutoShape as it is
     */
    public static IAutoShape renderAuxiliaryShape(IGroupShape group, float x, float y, Adjustment adjustment) {
        NodeProperties np = NodePropertiesFactory.transform(NodePropertiesFactory.get(x, y, 2, 2), adjustment.getFactor(), adjustment.getPanning());
        IAutoShape auxShape = group.getShapes().addAutoShape(ShapeType.Ellipse, np.getX().floatValue(), np.getY().floatValue(), np.getWidth().floatValue(), np.getHeight().floatValue());
        auxShape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        auxShape.getFillFormat().setFillType(FillType.NoFill);
        auxShape.getAutoShapeLock().setSizeLocked(true);
        return auxShape;
    }

    public static boolean touches(Shape shape, Coordinate c) {
        Coordinate centre;
        switch (shape.getType()) {
            case "CIRCLE":
            case "DOUBLE_CIRCLE":
                centre = shape.getC();
                return distance(centre, c) < shape.getR();
            case "BOX":
                Coordinate diff = shape.getB().minus(shape.getA());
                centre = shape.getA().add(diff.divide(2.0));
                return distance(centre, c) < diff.getX();
            default:
                throw new RuntimeException(shape.getType() + " hasn't been recognised.");
        }
    }

    private static double distance(Coordinate c1, Coordinate c2) {
        Coordinate diff = c2.minus(c1);
        return Math.sqrt(diff.getX() * diff.getX() + diff.getY() * diff.getY());
    }

    /**
     * Helper method to ease set the shape Style.
     *
     * @param shape the given shape to apply the style
     */
    public static void setShapeStyle(IShape shape, Stylesheet stylesheet) {
        // Fill properties
        shape.getFillFormat().setFillType(stylesheet.getShapeFillType());
        shape.getFillFormat().getSolidFillColor().setColor(stylesheet.getFillColor());

        // Line properties
        shape.getLineFormat().getFillFormat().setFillType(stylesheet.getLineFillType());
        shape.getLineFormat().getFillFormat().getSolidFillColor().setColor(stylesheet.getLineColor());
        shape.getLineFormat().setStyle(stylesheet.getLineStyle());
        shape.getLineFormat().setWidth(stylesheet.getLineWidth());
        shape.getLineFormat().setCapStyle(stylesheet.getLineCapStyle());

        if (stylesheet.getLineDashStyle() > 0) {
            shape.getLineFormat().setDashStyle(stylesheet.getLineDashStyle());
            shape.getLineFormat().setCapStyle(LineCapStyle.Square);
        }
    }

    /**
     * Helper method to ease set the shape Style.
     * Set includeBorder to true if you want the Rx with borders
     *
     * @param shape the given shape to apply the style
     */
    public static void setDrugShapeStyle(IShape shape, Stylesheet stylesheet, boolean includeBorders) {
        // Fill properties
        shape.getFillFormat().setFillType(stylesheet.getShapeFillType());
        shape.getFillFormat().getSolidFillColor().setColor(stylesheet.getFillColor());

        // Line properties
        if(includeBorders) {
            shape.getLineFormat().getFillFormat().setFillType(stylesheet.getLineFillType());
            shape.getLineFormat().getFillFormat().getSolidFillColor().setColor(stylesheet.getLineColor());
            shape.getLineFormat().setStyle(stylesheet.getLineStyle());
            shape.getLineFormat().setWidth(stylesheet.getLineWidth());
            shape.getLineFormat().setCapStyle(stylesheet.getLineCapStyle());
        } else {
            shape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        }

        if (stylesheet.getLineDashStyle() > 0) {
            shape.getLineFormat().setDashStyle(stylesheet.getLineDashStyle());
            shape.getLineFormat().setCapStyle(LineCapStyle.Square);
        }
    }
    /**
     * Helper method to ease set the flagging style of a group shape
     */
    public static void setFlaggingStyle(IShape shape, Stylesheet stylesheet) {
        IEffectFormat iEffectFormat = shape.getEffectFormat();
        iEffectFormat.enableGlowEffect();
        iEffectFormat.getGlowEffect().getColor().setColor(stylesheet.getFlagColor());
        iEffectFormat.getGlowEffect().setRadius(7);
    }

    /**
     * Helper method to ease set the selection style of an autoshape.
     */
    public static void setSelectedStyle(IAutoShape shape, Stylesheet stylesheet) {
        shape.getLineFormat().getFillFormat().setFillType(FillType.Solid);
        shape.getLineFormat().setStyle(LineStyle.Single);
        shape.getLineFormat().getFillFormat().getSolidFillColor().setColor(stylesheet.getSelectionColor());
        shape.getLineFormat().setWidth(3);
    }

    /**
     * Helper method to ease set the Begin Arrow style
     */
    static void setBeginArrowShape(IShape shape, Stylesheet stylesheet) {
        shape.getLineFormat().setBeginArrowheadLength(stylesheet.getLineArrowheadLength());
        shape.getLineFormat().setBeginArrowheadStyle(stylesheet.getLineArrowheadStyle());
        shape.getLineFormat().setBeginArrowheadWidth(stylesheet.getLineArrowheadWidth());
    }

    /**
     * Helper method to ease set the Begin Arrow style
     */
    public static void setEndArrowShape(IShape shape, byte arrowHeadLength, byte arrowHeadStyle, byte arrowHeadWidth) {
        shape.getLineFormat().setEndArrowheadLength(arrowHeadLength);
        shape.getLineFormat().setEndArrowheadStyle(arrowHeadStyle);
        shape.getLineFormat().setEndArrowheadWidth(arrowHeadWidth);
    }

    /**
     * Helper method to ease set text properties, link, margin.
     *
     * @param margins      0-top, 1-left, 2-bottom, 3-right
     * @param addHyperlink flag to add a hyperlink
     * @param stId         identifier to build the link
     */
    public static void setTextFrame(IAutoShape shape,
                                    String text,
                                    double[] margins,
                                    Color fontColor,
                                    float fontHeight,
                                    boolean bold,
                                    boolean addHyperlink,
                                    Long stId,
                                    Adjustment adjustment) {

        shape.addTextFrame("");

        ITextFrame txtFrame = shape.getTextFrame();
        txtFrame.getTextFrameFormat().setMarginTop(margins[0]);
        txtFrame.getTextFrameFormat().setMarginLeft(margins[1]);
        txtFrame.getTextFrameFormat().setMarginBottom(margins[2]);
        txtFrame.getTextFrameFormat().setMarginRight(margins[3]);

        IPortion portion = txtFrame.getParagraphs().get_Item(0).getPortions().get_Item(0);
        portion.getPortionFormat().setFontHeight(((float) (fontHeight * adjustment.getFactor())));
        portion.getPortionFormat().getFillFormat().setFillType(FillType.Solid);

        if (bold) portion.getPortionFormat().setFontBold(NullableBool.True);

        if (addHyperlink) {
            if (stId != null && stId >= 0) { // Avoiding wrong links to be generated
                // Having the link is not possible to change the font color
                portion.getPortionFormat().getHyperlinkManager().setExternalHyperlinkClick("https://reactome.org/content/detail/" + stId);
            }
        }

        portion.getPortionFormat().getFillFormat().getSolidFillColor().setColor(fontColor);
        portion.setText(text);
    }

    /**
     * Render the Activator Shape. PowerPoint does not support the shape we need to achieve. So we are just
     * following the coordinates of an arrow end shape.
     *
     * @return the centre shape to attach the anchor Point
     */
    private static IAutoShape renderActivatorShape(IShapeCollection shapes, IGroupShape groupShape, Shape shape, Stylesheet stylesheet, Adjustment adjustment) {
        NodeProperties npA = NodePropertiesFactory.transform(NodePropertiesFactory.get(shape.getA().getX().floatValue(), shape.getA().getY().floatValue(), 0.1f, 0.1f), adjustment.getFactor(), adjustment.getPanning());
        IAutoShape a = groupShape.getShapes().addAutoShape(
                ShapeType.Line,
                npA.getX().floatValue(),
                npA.getY().floatValue(),
                npA.getWidth().floatValue(),
                npA.getHeight().floatValue()
        );
        a.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        a.getFillFormat().setFillType(FillType.NoFill);

        NodeProperties npB = NodePropertiesFactory.transform(NodePropertiesFactory.get(shape.getB().getX().floatValue(), shape.getB().getY().floatValue(), 0.1f, 0.1f), adjustment.getFactor(), adjustment.getPanning());
        IAutoShape b = groupShape.getShapes().addAutoShape(
                ShapeType.Line,
                npB.getX().floatValue(),
                npB.getY().floatValue(),
                npB.getWidth().floatValue(),
                npB.getHeight().floatValue()
        );
        b.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        b.getFillFormat().setFillType(FillType.NoFill);

        NodeProperties npC = NodePropertiesFactory.transform(NodePropertiesFactory.get(shape.getC().getX().floatValue(), shape.getC().getY().floatValue(), 0.1f, 0.1f), adjustment.getFactor(), adjustment.getPanning());
        IAutoShape c = groupShape.getShapes().addAutoShape(
                ShapeType.Line,
                npC.getX().floatValue(),
                npC.getY().floatValue(),
                npC.getWidth().floatValue(),
                npC.getHeight().floatValue()
        );
        c.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        c.getFillFormat().setFillType(FillType.NoFill);

        SegmentUtil.drawSegment(shapes, a, b, stylesheet);
        SegmentUtil.drawSegment(shapes, b, c, stylesheet);
        SegmentUtil.drawSegment(shapes, c, a, stylesheet);

        float x = (shape.getA().getX().floatValue() + shape.getC().getX().floatValue()) / 2f;
        float y = (shape.getA().getY().floatValue() + shape.getC().getY().floatValue()) / 2f;

        IAutoShape centre = renderAuxiliaryShape(groupShape, x, y, adjustment);
        centre.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        centre.getFillFormat().setFillType(FillType.NoFill);

        return centre;
    }

    /**
     * Draw catalyst shape inside the group shape of a reaction
     */
    public static void drawCatalyst(Map<Connector, IAutoShape> shapeMap, IGroupShape groupShape, Connector connector, Stylesheet s, Adjustment adjustment) {
        Color auxShapeFillColor = Color.BLACK;
        if (connector.getIsDisease() != null && connector.getIsDisease()) {
            s.setLineColor(Color.RED);
            auxShapeFillColor = Color.RED;
        }

        Shape shape = connector.getEndShape();
        IAutoShape catalystAnchorPoint = renderAuxiliaryShape(groupShape, shape.getC(), new Stylesheet().customStyle(1, LineStyle.NotDefined, FillType.NotDefined, s.getLineColor(), FillType.Solid, auxShapeFillColor, s.getLineDashStyle()), adjustment);

        // set white filling for the catalyst, renderShape will render the catalyst properly based on the stylesheet
        renderShape(groupShape, shape, new Stylesheet().customStyle(s.getLineWidth(), s.getLineStyle(), s.getLineFillType(), s.getLineColor(), s.getShapeFillType(), Color.WHITE, s.getLineDashStyle()), adjustment);
        shapeMap.put(connector, catalystAnchorPoint);
    }

    public static void drawActivator(IShapeCollection shapes, Map<Connector, IAutoShape> shapeMap, IGroupShape groupShape, Connector connector, Stylesheet stylesheet, Adjustment adjustment) {
        if (connector.getIsDisease() != null && connector.getIsDisease()) {
            stylesheet.setLineColor(Color.RED);
        }
        Shape shape = connector.getEndShape();
        IAutoShape centre = renderActivatorShape(shapes, groupShape, shape, stylesheet, adjustment);
        shapeMap.put(connector, centre);
    }

    /**
     * Draw inhibitor line inside the group shape of a reaction.
     * A line shape in power point isn't straight to control. Then we take the points provided by the JSON
     * and connect them, calculate the centre and add a auxiliary shape.
     * ShapeMap holds the shape where the connector (in this case type Inhibitor) will attach.
     */
    public static void drawInhibitor(IShapeCollection shapes, Map<Connector, IAutoShape> shapeMap, IGroupShape groupShape, Connector connector, Stylesheet stylesheet, Adjustment adjustment) {
        if (connector.getIsDisease() != null && connector.getIsDisease()) {
            stylesheet.setLineColor(Color.RED);
        }
        Shape shape = connector.getEndShape();
        // IMPORTANT: A line didn't work here. Has to be two connected shapes
        NodeProperties np = NodePropertiesFactory.transform(NodePropertiesFactory.get(shape.getA().getX().floatValue(), shape.getA().getY().floatValue(), 0.1f, 0.1f), adjustment.getFactor(), adjustment.getPanning());
        IAutoShape a = groupShape.getShapes().addAutoShape(
                ShapeType.Line,
                np.getX().floatValue(),
                np.getY().floatValue(),
                np.getWidth().floatValue(),
                np.getHeight().floatValue()
        );
        a.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        a.getFillFormat().setFillType(FillType.NoFill);

        np = NodePropertiesFactory.transform(NodePropertiesFactory.get(shape.getB().getX().floatValue(), shape.getB().getY().floatValue(), 0.1f, 0.1f), adjustment.getFactor(), adjustment.getPanning());
        IAutoShape b = groupShape.getShapes().addAutoShape(
                ShapeType.Line,
                np.getX().floatValue(),
                np.getY().floatValue(),
                np.getWidth().floatValue(),
                np.getHeight().floatValue()
        );
        b.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        b.getFillFormat().setFillType(FillType.NoFill);

        np = NodePropertiesFactory.transform(NodePropertiesFactory.get(shape.getC().getX().floatValue(), shape.getC().getY().floatValue(), 0.1f, 0.1f), adjustment.getFactor(), adjustment.getPanning());
        IAutoShape centre = groupShape.getShapes().addAutoShape(
                ShapeType.Line,
                np.getX().floatValue(),
                np.getY().floatValue(),
                np.getWidth().floatValue(),
                np.getHeight().floatValue()
        );
        centre.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
        centre.getFillFormat().setFillType(FillType.NoFill);

        drawSegment(shapes, a, b, stylesheet);
        shapeMap.put(connector, centre);
    }

    /**
     * Draw (pptx)stoichiometry shape based on the stoichiometry object provided by the connector.
     *
     * @return null if stoichiometry is null or value 1, otherwise stoichiometry shape
     */
    public static PPTXStoichiometry drawStoichiometry(IShapeCollection shapes, Stoichiometry stoichiometry, Stylesheet stylesheet, Adjustment adjustment) {
        // Stoichiometry may be present, but having value 1. In this case we don't render it.
        if (stoichiometry == null || stoichiometry.getValue() == 1) return null;

        return new PPTXStoichiometry(stoichiometry, adjustment).render(shapes, stylesheet);
    }

    public static void reorder(IShapeCollection shapes, Collection<PPTXNode> nodes) {
        for (PPTXNode node : nodes) {
            shapes.reorder(shapes.size() - 1, node.getiGroupShape());
        }
    }

    public static void reorder(IShapeCollection shapes, IGroupShape groupShape) {
        shapes.reorder(shapes.size() - 1, groupShape);
    }
}
