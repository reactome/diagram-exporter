package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.*;
import org.reactome.server.tools.diagram.data.layout.Connector;
import org.reactome.server.tools.diagram.data.layout.*;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfile;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.*;
import static org.reactome.server.tools.diagram.exporter.pptx.util.SegmentUtil.connect;
import static org.reactome.server.tools.diagram.exporter.pptx.util.SegmentUtil.drawSegment;

/**
 * @author Antonio Fabregat (fabregat@ebi.ac.uk)
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */
public class PPTXReaction {

    private static final String PROFILE_TYPE = "reaction";
    private Edge edge;
    private Map<Long, PPTXNode> nodesMap;

    private IAutoShape backboneStart;
    private IAutoShape backboneEnd;

    private IAutoShape hiddenReactionShape; // hidden behind the reactionShape
    private Map<Connector, IAutoShape> shapeMap = new HashMap<>();
    private DiagramProfile profile;
    private Adjustment adjustment;
    private boolean selected;

    public PPTXReaction(Edge edge, Map<Long, PPTXNode> nodesMap, DiagramProfile profile, Adjustment adjustment, boolean selected) {
        this.edge = edge;
        this.nodesMap = nodesMap;
        this.profile = profile;
        this.adjustment = adjustment;
        this.selected = selected;
    }

    public void render(IShapeCollection shapes) {
        Stylesheet stylesheet = new Stylesheet(profile, PROFILE_TYPE, FillType.Solid, FillType.Solid, LineStyle.Single);
        stylesheet.setFillColor(Color.black); // fill color is white in the profile
        stylesheet.setTextColor(Color.black); // text color is white in the profile

        if (selected) {
            // if the reaction is selected, every element that belongs to the reaction will be styled as 'selected'!
            // changing in the stylesheet object implies.
            stylesheet.setLineFillType(FillType.Solid);
            stylesheet.setLineStyle(LineStyle.Single);
            stylesheet.setLineColor(stylesheet.getSelectionColor());
            stylesheet.setLineWidth(stylesheet.getSelectionLineWidth());
        }

        // It checks for all the shapes to be grouped and creates them in advance placing
        // the main shape in reactionShape and the rest in the shapeMap map
        IGroupShape reactionGroupShape = createReactionShape(shapes, stylesheet);
        if (hasInputs()) {
            if (onlyOneInputWithoutConnectors()) {
                PPTXNode inputNode = nodesMap.get(edge.getInputs().get(0).getId());
                IAutoShape input = inputNode.getiAutoShape();

                Connector connector = inputNode.getConnectors(edge.getId(), "INPUT").get(0);
                setConnectorStyle(connector, stylesheet);

                PPTXStoichiometry stoich = drawStoichiometry(shapes, connector.getStoichiometry(), stylesheet, adjustment);
                // we cannot reuse the connectToStoichiometry
                if (stoich == null) {
                    connect(shapes, inputNode, input, backboneStart, false, stylesheet);
                } else {
                    connect(shapes, inputNode, input, stoich.getHiddenCenterShape(), false, stylesheet);
                    drawSegment(shapes, stoich.getHiddenCenterShape(), hiddenReactionShape, stylesheet);
                    reorder(shapes, stoich.getiGroupShape());
                }
            } else {
                createConnectorsFromInputs(shapes, stylesheet);
            }
        }

        if (hasOutputs()) {
            if (onlyOneOutputWithoutConnectors()) {
                PPTXNode outputNode = nodesMap.get(edge.getOutputs().get(0).getId());
                IAutoShape output = outputNode.getiAutoShape();

                Connector connector = outputNode.getConnectors(edge.getId(), "OUTPUT").get(0);
                setConnectorStyle(connector, stylesheet);

                connect(shapes, outputNode, output, backboneEnd, true, stylesheet); // render Arrow,
            } else {
                createConnectorsToOutputs(shapes, stylesheet);
            }
        }

        if (hasCatalyst()) createConnectorsToCatalyst(shapes, stylesheet);

        if (hasActivators()) createConnectorsToActivator(shapes, stylesheet);

        if (hasInhibitors()) createConnectorsToInhibitor(shapes, stylesheet);

        // Bring reaction shape to front after rendering I/O/C/A/Inhibitors
        shapes.reorder(shapes.size() - 1, reactionGroupShape);

    }

    private IGroupShape createReactionShape(IShapeCollection shapes, Stylesheet stylesheet) {
        //Creates reaction shape
        IGroupShape groupShape = shapes.addGroupShape();
        Shape rShape = edge.getReactionShape();

        hiddenReactionShape = renderAuxiliaryShape(groupShape, edge.getPosition(), stylesheet, adjustment);
        if (edge.getIsFadeOut() != null) {
            stylesheet.setLineColor(stylesheet.getFadeOutStroke());
            stylesheet.setFillColor(stylesheet.getFadeOutFill());
        }
        if (edge.getIsDisease() != null && edge.getIsDisease()) {
            stylesheet.setLineColor(stylesheet.getDiseaseColor());
            stylesheet.setFillColor(stylesheet.getDiseaseColor());
            stylesheet.setTextColor(stylesheet.getDiseaseColor());
        }

        // rendering reaction shape, we don't need an instance of the shape itself, just rendering
        // the most important is the hiddenReactionShape which is a centre shape for the reaction used to anchor.
        renderShape(groupShape, rShape, stylesheet, adjustment);

        createBackBone(shapes, rShape, stylesheet);

        if (hasCatalyst()) {
            for (ReactionPart reactionPart : edge.getCatalysts()) {
                PPTXNode catalyst = nodesMap.get(reactionPart.getId());
                for (Connector connector : catalyst.getConnectors()) {
                    if (!isType(connector, "CATALYST")) continue;
                    if (connector.getEndShape() != null) {
                        drawCatalyst(shapeMap, groupShape, connector, stylesheet, adjustment);
                    }
                }
            }
        }

        // NOTE 1: Is better not to draw the Activators shape and use the "end-arrow" in the last segment instead
        // NOTE 2: Could find any arrow shape for the Activators.
        if (hasActivators()) {
            for (ReactionPart reactionPart : edge.getActivators()) {
                PPTXNode activator = nodesMap.get(reactionPart.getId());
                for (Connector connector : activator.getConnectors()) {
                    if (!isType(connector, "ACTIVATOR")) continue;
                    drawActivator(shapes, shapeMap, groupShape, connector, stylesheet, adjustment);
                }
            }
        }

        if (hasInhibitors()) {
            for (ReactionPart reactionPart : edge.getInhibitors()) {
                PPTXNode inhibitor = nodesMap.get(reactionPart.getId());
                for (Connector connector : inhibitor.getConnectors()) {
                    if (!isType(connector, "INHIBITOR")) continue;
                    drawInhibitor(shapes, shapeMap, groupShape, connector, stylesheet, adjustment);
                }
            }
        }
        groupShape.getGroupShapeLock().setSizeLocked(true);
        return groupShape;
    }

    /**
     * The backbone is a little bit special since it might contain a number of segments and the reaction shape
     * will be in the intersection of either of them.
     * <p>
     * The problem is that initially we don't know which one because the diagram has always been drawn in
     * raster and that was not a big problem. Now we have to figure out which connector connects to the
     * reaction shape, and that's what this method is doing (don't panic!)
     *
     * @param shapes the collection of shapes where the backbone has to be added
     * @param rShape the reaction shape previously created
     */
    private void createBackBone(IShapeCollection shapes, Shape rShape, Stylesheet stylesheet) {
        Coordinate start = edge.getSegments().isEmpty() ? edge.getPosition() : edge.getSegments().get(0).getFrom();

        if (touches(rShape, start)) {
            backboneStart = hiddenReactionShape;
        } else {
            backboneStart = renderAuxiliaryShape(shapes, start, stylesheet, adjustment);
        }

        IAutoShape last = backboneStart;
        for (int i = 1; i < edge.getSegments().size(); i++) { //IMPORTANT: It starts in "1" because "0" has been taken above
            Coordinate step = edge.getSegments().get(i).getFrom();
            if (touches(rShape, step)) {
                drawSegment(shapes, last, hiddenReactionShape, stylesheet);
                last = hiddenReactionShape;
            } else {
                IAutoShape backboneStep = renderAuxiliaryShape(shapes, step, stylesheet, adjustment);
                drawSegment(shapes, last, backboneStep, stylesheet);
                last = backboneStep;
            }
        }
        backboneEnd = last; //The last one could either be an anchor point or the reactionShape itself, but that's not a problem at all!
    }

    private void createConnectorsFromInputs(IShapeCollection shapes, Stylesheet stylesheet) {
        for (ReactionPart reactionPart : edge.getInputs()) {
            PPTXNode input = nodesMap.get(reactionPart.getId());
            IAutoShape last = input.getiAutoShape();
            for (Connector connector : input.getConnectors()) {
                if (!isType(connector, "INPUT")) continue;
                createReactionAttributes(shapes, connector, input, last, backboneStart, stylesheet);
            }
        }
    }

    private void createConnectorsToOutputs(IShapeCollection shapes, Stylesheet stylesheet) {
        for (ReactionPart reactionPart : edge.getOutputs()) {
            PPTXNode output = nodesMap.get(reactionPart.getId());
            IAutoShape last = output.getiAutoShape();
            for (Connector connector : output.getConnectors()) {
                if (!isType(connector, "OUTPUT")) continue;
                boolean hasSegments = false;

                PPTXStoichiometry stoich = drawStoichiometry(shapes, connector.getStoichiometry(), stylesheet, adjustment);

                for (int i = 1; i < connector.getSegments().size(); i++) {
                    Segment segment = connector.getSegments().get(i);
                    IAutoShape step = renderAuxiliaryShape(shapes, segment.getFrom(), stylesheet, adjustment); //shapes.addAutoShape(ShapeType.Ellipse, segment.getFrom().getX().floatValue(), segment.getFrom().getY().floatValue(), 1f, 1f);

                    // As we are connecting from node to segment, then the first step should
                    // have the arrow and stoichiometry
                    if (i == 1) { // first step checks the anchor point
                        connectToStoichiometry(shapes, output, stoich, last, step, true, stylesheet);
                    } else {
                        drawSegment(shapes, last, step, stylesheet);
                    }
                    last = step;
                    hasSegments = true;
                }

                if (!hasSegments) {
                    connectToStoichiometry(shapes, output, stoich, last, backboneEnd, true, stylesheet);
                } else {
                    drawSegment(shapes, last, backboneEnd, stylesheet);
                }
            }
        }
    }

    private void createConnectorsToCatalyst(IShapeCollection shapes, Stylesheet stylesheet) {
        for (ReactionPart reactionPart : edge.getCatalysts()) {
            PPTXNode catalyst = nodesMap.get(reactionPart.getId());
            IAutoShape last = catalyst.getiAutoShape();
            for (Connector connector : catalyst.getConnectors()) {
                if (!isType(connector, "CATALYST")) continue;
                if (connector.getEndShape() != null) {
                    createReactionAttributes(shapes, connector, catalyst, last, shapeMap.get(connector), stylesheet);
                }
            }
        }
    }

    private void createConnectorsToActivator(IShapeCollection shapes, Stylesheet stylesheet) {
        for (ReactionPart reactionPart : edge.getActivators()) {
            PPTXNode activator = nodesMap.get(reactionPart.getId());
            IAutoShape last = activator.getiAutoShape();
            for (Connector connector : activator.getConnectors()) {
                if (!isType(connector, "ACTIVATOR")) continue;
                createReactionAttributes(shapes, connector, activator, last, shapeMap.get(connector), stylesheet); // reactionShape
            }
        }
    }

    private void createConnectorsToInhibitor(IShapeCollection shapes, Stylesheet stylesheet) {
        for (ReactionPart reactionPart : edge.getInhibitors()) {
            PPTXNode inhibitor = nodesMap.get(reactionPart.getId());
            IAutoShape last = inhibitor.getiAutoShape();
            for (Connector connector : inhibitor.getConnectors()) {
                if (!isType(connector, "INHIBITOR")) continue;
                createReactionAttributes(shapes, connector, inhibitor, last, shapeMap.get(connector), stylesheet);
            }
        }
    }

    /**
     * A connector may have stoichiometry value greater than 1, in these cases we should render the Stoichiometry
     * box with the value and properly connect the segment to the stoichiometry shape. So, if stoichiometry is not present
     * just render a normal connector, otherwise the segment goes from its start point to stoichiometry hiddenShape
     * then hiddenShape to end node.
     */
    private void connectToStoichiometry(IShapeCollection shapes, PPTXNode node, PPTXStoichiometry stoich, IShape start, IShape end, boolean renderArrow, Stylesheet stylesheet) {
        if (stoich == null) {
            connect(shapes, node, start, end, renderArrow, stylesheet);
        } else {
            connect(shapes, node, start, stoich.getHiddenCenterShape(), renderArrow, stylesheet);
            drawSegment(shapes, stoich.getHiddenCenterShape(), end, stylesheet);
            reorder(shapes, stoich.getiGroupShape());
        }
    }

    /**
     * Common method used to draw the segments for Input, Catalyst, Inhibitor and Activator as well as
     * rendering the Stoichiometry, if also present.
     */
    private void createReactionAttributes(IShapeCollection shapes, Connector connector, PPTXNode pptxNode, IAutoShape start, IAutoShape end, Stylesheet stylesheet) {
        setConnectorStyle(connector, stylesheet);

        PPTXStoichiometry stoich = drawStoichiometry(shapes, connector.getStoichiometry(), stylesheet, adjustment);

        boolean hasSegments = false;
        IAutoShape last = start; // not really need, just to make it easy to understand
        for (int i = 0; i < connector.getSegments().size() - 1; i++) {
            Segment segment = connector.getSegments().get(i);
            IAutoShape step = renderAuxiliaryShape(shapes, segment.getTo(), stylesheet, adjustment);
            if (i == 0) { // first step checks the anchor point
                connectToStoichiometry(shapes, pptxNode, stoich, last, step, false, stylesheet);
            } else {
                drawSegment(shapes, last, step, stylesheet);
            }
            last = step;
            hasSegments = true;
        }

        if (!hasSegments) {
            connectToStoichiometry(shapes, pptxNode, stoich, last, end, false, stylesheet);
        } else {
            drawSegment(shapes, last, end, stylesheet);
        }
    }

    /**
     * Setting connector style in case it is "fadeOut" or "isDisease".
     * It is not returning anything but change the line colors depends on the conditions.
     */
    private void setConnectorStyle(Connector connector, Stylesheet stylesheet) {
        if (connector.getIsFadeOut() != null) {
            stylesheet.setLineColor(stylesheet.getFadeOutStroke());
        }
        // disease has to be set after the fadeOut, then we make sure the red line is on top of the fadeOut
        if (connector.getIsDisease() != null && connector.getIsDisease()) {
            stylesheet.setLineColor(Color.RED);
            stylesheet.setFillColor(Color.RED);
            stylesheet.setTextColor(Color.RED);
        }
    }

    private boolean onlyOneInputWithoutConnectors() {
        if (edge.getInputs().size() == 1) {
            List<Connector> connectors = nodesMap.get(edge.getInputs().get(0).getId()).getConnectors(edge.getId(), "INPUT");
            return connectors.isEmpty() || connectors.get(0).getSegments().isEmpty();
        }
        return false;
    }

    private boolean onlyOneOutputWithoutConnectors() {
        if (edge.getOutputs().size() == 1) {
            List<Connector> connectors = nodesMap.get(edge.getOutputs().get(0).getId()).getConnectors(edge.getId(), "OUTPUT");
            return connectors.isEmpty() || connectors.get(0).getSegments().isEmpty();
        }
        return false;
    }

    private boolean hasInputs() {
        return edge.getInputs() != null && !edge.getInputs().isEmpty();
    }

    private boolean isType(Connector connector, String type) {
        return Objects.equals(connector.getEdgeId(), edge.getId()) && Objects.equals(connector.getType(), type);
    }

    private boolean hasOutputs() {
        return edge.getOutputs() != null && !edge.getOutputs().isEmpty();
    }

    private boolean hasCatalyst() {
        return edge.getCatalysts() != null && !edge.getCatalysts().isEmpty();
    }

    private boolean hasActivators() {
        return edge.getActivators() != null && !edge.getActivators().isEmpty();
    }

    private boolean hasInhibitors() {
        return edge.getInhibitors() != null && !edge.getInhibitors().isEmpty();
    }

}
