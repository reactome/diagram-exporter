package org.reactome.server.tools.diagram.exporter.raster.diagram.renderables;

import org.reactome.server.analysis.core.model.AnalysisType;
import org.reactome.server.tools.diagram.data.layout.EdgeCommon;
import org.reactome.server.tools.diagram.data.layout.NodeProperties;
import org.reactome.server.tools.diagram.data.layout.Segment;
import org.reactome.server.tools.diagram.data.layout.Shape;
import org.reactome.server.tools.diagram.data.layout.impl.NodePropertiesFactory;
import org.reactome.server.tools.diagram.exporter.raster.diagram.common.DiagramData;
import org.reactome.server.tools.diagram.exporter.raster.diagram.common.FontProperties;
import org.reactome.server.tools.diagram.exporter.raster.diagram.common.ShapeFactory;
import org.reactome.server.tools.diagram.exporter.raster.diagram.common.StrokeStyle;
import org.reactome.server.tools.diagram.exporter.raster.diagram.layers.DiagramCanvas;
import org.reactome.server.tools.diagram.exporter.raster.diagram.layers.DrawLayer;
import org.reactome.server.tools.diagram.exporter.raster.diagram.layers.FillDrawLayer;
import org.reactome.server.tools.diagram.exporter.raster.diagram.layers.TextLayer;
import org.reactome.server.tools.diagram.exporter.raster.profiles.ColorProfiles;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains extra rendering information for an edge: decorators plus
 * connectors.
 */
public abstract class RenderableEdgeCommon<T extends EdgeCommon> extends RenderableDiagramObject<T> {

	RenderableEdgeCommon(T edge) {
		super(edge);
	}

	public T getEdge() {
		return getDiagramObject();
	}

	public boolean isDashed() {
		return false;
	}

	@Override
	public void draw(DiagramCanvas canvas, ColorProfiles colorProfiles, DiagramData data, int t) {
		final Color linesColor = getStrokeColor(colorProfiles, data.getAnalysis().getType());
		final Color fillColor = getFillColor(colorProfiles, data.getAnalysis().getType());
		segments(linesColor, canvas, colorProfiles);
		shapes(linesColor, canvas, colorProfiles);
	}

	@Override
	protected Color getFillColor(ColorProfiles colorProfiles, AnalysisType type) {
		//fadeout -> analysis -> normal
		if (isFadeOut())
			return getColorProfile(colorProfiles).getFadeOutFill();
		if (type != null) {
			// report: lighterfill of reaction is grey (modern profile) as text
			return getColorProfile(colorProfiles).getFill();
		}
		return getColorProfile(colorProfiles).getFill();
	}

	private void segments(Color linesColor, DiagramCanvas canvas, ColorProfiles colorProfiles) {
		final Collection<java.awt.Shape> segments = createSegments();
		if (isHalo()) {
			segments.forEach(shape -> canvas.getHalo().add(shape,
					colorProfiles.getDiagramSheet().getProperties().getHalo(),
					StrokeStyle.HALO.get(isDashed())
			));
		}
		if (isFlag()) {
			segments.forEach(shape -> canvas.getFlags().add(shape,
					colorProfiles.getDiagramSheet().getProperties().getFlag(),
					StrokeStyle.FLAG.get(isDashed())
			));
		}
		final DrawLayer layer = isFadeOut()
				? canvas.getFadeOutSegments()
				: canvas.getSegments();
		final Stroke stroke = isSelected()
				? StrokeStyle.SELECTION.get(isDashed())
				: StrokeStyle.SEGMENT.get(isDashed());
		segments.forEach(shape -> layer.add(shape, linesColor, stroke));
	}

	private void shapes(Color linesColor, DiagramCanvas canvas, ColorProfiles colorProfiles) {
		final FillDrawLayer layer = isFadeOut()
				? canvas.getFadeOutEdgeShapes()
				: canvas.getEdgeShapes();
		final TextLayer textLayer = isFadeOut()
				? canvas.getFadeOutText()
				: canvas.getText();
		final Stroke stroke = StrokeStyle.SEGMENT.getNormal(); // shapes not dashed
		getRenderableShapes().forEach(shape -> drawShape(linesColor, canvas, colorProfiles, layer, textLayer, stroke, shape));
	}

	private void drawShape(Color linesColor, DiagramCanvas canvas, ColorProfiles colorProfiles, FillDrawLayer layer, TextLayer textLayer, Stroke stroke, Shape shape) {
		if (shape == null) return;
		final java.awt.Shape awtShape = ShapeFactory.getShape(shape);
		if (isFlag()) flag(canvas, colorProfiles, awtShape);
		if (isHalo()) halo(canvas, colorProfiles, awtShape);
		final Color color = shape.getEmpty() != null && shape.getEmpty()
				? Color.WHITE
				: linesColor;
		// shapes use border color for filling
		// https://github.com/reactome/pwp-diagram/blob/dev/src/main/java/org/reactome/web/diagram/renderers/layout/abs/ShapeAbstractRenderer.java#L87
		// ctx.setFillStyle(ctx.getStrokeStyle());
		layer.add(awtShape, color, linesColor, stroke);
		if (shape.getType().equals("DOUBLE_CIRCLE"))
			layer.add(ShapeFactory.innerCircle(shape), color, linesColor, stroke);
		if (shape.getS() != null && !shape.getS().isEmpty()) {
			final NodeProperties limits = NodePropertiesFactory.get(
					shape.getA().getX(), shape.getA().getY(),
					shape.getB().getX() - shape.getA().getX(),
					shape.getB().getY() - shape.getA().getY());
			textLayer.add(shape.getS(), linesColor, limits, 1, 0, FontProperties.DEFAULT_FONT);
		}
	}

	private void flag(DiagramCanvas canvas, ColorProfiles colorProfiles, java.awt.Shape awtShape) {
		canvas.getFlags().add(awtShape,
				colorProfiles.getDiagramSheet().getProperties().getFlag(),
				StrokeStyle.FLAG.get(isDashed()));
	}

	private void halo(DiagramCanvas canvas, ColorProfiles colorProfiles, java.awt.Shape awtShape) {
		canvas.getHalo().add(awtShape,
				colorProfiles.getDiagramSheet().getProperties().getHalo(),
				StrokeStyle.HALO.get(isDashed()));
	}

	private Collection<java.awt.Shape> createSegments() {
		final Collection<java.awt.Shape> segments = new LinkedList<>();
		for (Segment segment : getEdge().getSegments())
			segments.add(ShapeFactory.createLine(segment.getFrom(), segment.getTo()));
		return segments;
	}

	public List<Shape> getRenderableShapes() {
		final List<Shape> shapes = new ArrayList<>();
		if (getEdge().getEndShape() != null) shapes.add(getEdge().getEndShape());
		if (getEdge().getReactionShape() != null) shapes.add(getEdge().getReactionShape());
		return shapes;
	}

	@Override
	Color getStrokeColor(ColorProfiles colorProfiles, AnalysisType type) {
		// Note edges lines do not use analysis color (lighterStroke)
		// selection -> disease -> fadeout -> normal
		if (isSelected())
			return colorProfiles.getDiagramSheet().getProperties().getSelection();
		if (isDisease())
			return colorProfiles.getDiagramSheet().getProperties().getDisease();
		if (isFadeOut())
			return getColorProfile(colorProfiles).getFadeOutStroke();
		return getColorProfile(colorProfiles).getStroke();
	}

}
