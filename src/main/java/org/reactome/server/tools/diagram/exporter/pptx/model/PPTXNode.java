package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.*;
import org.reactome.server.tools.diagram.data.layout.Connector;
import org.reactome.server.tools.diagram.data.layout.Node;
import org.reactome.server.tools.diagram.exporter.common.profiles.model.DiagramProfile;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.setShapeStyle;
import static org.reactome.server.tools.diagram.exporter.pptx.util.PPTXShape.setTextFrame;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 * @author Guilherme S Viteri <gviteri@ebi.ac.uk>
 */
public abstract class PPTXNode {

    @SuppressWarnings("WeakerAccess")
    protected IAutoShape iAutoShape;
    protected IGroupShape iGroupShape;
    protected float x;
    protected float y;
    protected float width = 1;
    protected float height = 1;
    protected Long reactomeId;
    protected String displayName;
    protected boolean isFadeOut;
    private Long id;
    private String schemaClass;
    private List<Connector> connectors;
    private boolean isDisease;
    private boolean isCrossed;
    private boolean needDashedBorder;

    public PPTXNode(Node node) {
        this.reactomeId = node.getReactomeId();
        this.schemaClass = node.getSchemaClass();

        this.id = node.getId();
        this.x = node.getProp().getX().floatValue();
        this.y = node.getProp().getY().floatValue();
        this.width = node.getProp().getWidth().floatValue();
        this.height = node.getProp().getHeight().floatValue();
        this.displayName = node.getDisplayName();
        this.connectors = node.getConnectors();
        this.isDisease = node.getIsDisease() == null ? false : node.getIsDisease();
        this.isCrossed = node.getIsCrossed() == null ? false : node.getIsCrossed();
        this.isFadeOut = node.getIsFadeOut() == null ? false : node.getIsFadeOut();
        this.needDashedBorder = node.getNeedDashedBorder() == null ? false : node.getNeedDashedBorder();
    }

    public Long getId() {
        return id;
    }

    public IAutoShape getiAutoShape() {
        return iAutoShape;
    }

    public IGroupShape getiGroupShape() {
        return iGroupShape;
    }

    public List<Connector> getConnectors() {
        return connectors != null ? connectors : new LinkedList<>();
    }

    public List<Connector> getConnectors(Long edgeId, String type) {
        return getConnectors().stream().filter(connector -> Objects.equals(connector.getEdgeId(), edgeId) && Objects.equals(connector.getType(), type)).collect(Collectors.toList());
    }

    public abstract void render(IShapeCollection shapes, DiagramProfile profile);

    final void render(IShapeCollection shapes, int shapeType, Stylesheet stylesheet) {
        iGroupShape = shapes.addGroupShape();
        iAutoShape = iGroupShape.getShapes().addAutoShape(shapeType, x, y, width, height);

        if (isFadeOut) {
            stylesheet.setFillColor(stylesheet.getFadeOutFill());
            stylesheet.setLineColor(stylesheet.getFadeOutStroke());
            stylesheet.setTextColor(stylesheet.getTextColor());
        }

        if (isCrossed) {
            IAutoShape a = iGroupShape.getShapes().addAutoShape(ShapeType.Line, x, y, width, height);
            IAutoShape b = iGroupShape.getShapes().addAutoShape(ShapeType.Line, x, y, width, height);
            b.setRotation(2f * (float) Math.toDegrees(Math.atan2(width, height)));
            stylesheet.setLineColor(Color.RED);
            setShapeStyle(a, stylesheet);
            setShapeStyle(b, stylesheet);
        }

        if (needDashedBorder) {
            stylesheet.setLineStyle(LineStyle.ThinThin);
            stylesheet.setLineWidth(3.5);
            stylesheet.setLineDashStyle(LineDashStyle.Dash);
        }

        // has to be last one :)
        if (isDisease) {
            stylesheet.setLineColor(Color.RED);
        }

        setShapeStyle(iAutoShape, stylesheet);
        setTextFrame(iAutoShape, displayName, new double[]{0, 0, 0, 0}, stylesheet.getTextColor(), 10, true, true, reactomeId);
    }

    @Override
    public String toString() {
        return schemaClass + "{" +
                "id=" + reactomeId +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
