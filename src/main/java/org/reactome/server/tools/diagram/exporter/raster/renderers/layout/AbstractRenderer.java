package org.reactome.server.tools.diagram.exporter.raster.renderers.layout;

import org.reactome.server.tools.diagram.data.layout.DiagramObject;
import org.reactome.server.tools.diagram.exporter.raster.renderers.common.AdvancedGraphics2D;

import java.awt.*;
import java.util.Collection;

/**
 *
 *
 */
public abstract class AbstractRenderer implements Renderer {


	@Override
	public void drawEnrichments(AdvancedGraphics2D graphics, DiagramObject item) {

	}

	@Override
	public void drawExpression(AdvancedGraphics2D graphics, DiagramObject item) {

	}

	@Override
	public void drawHitInteractors(AdvancedGraphics2D graphics, DiagramObject item) {

	}

	@Override
	public void draw(AdvancedGraphics2D graphics, Collection<? extends DiagramObject> items, Paint fillColor, Paint lineColor, Paint textColor, Stroke segmentStroke, Stroke borderStroke) {

	}

}
