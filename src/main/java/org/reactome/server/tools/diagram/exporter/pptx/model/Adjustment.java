package org.reactome.server.tools.diagram.exporter.pptx.model;

import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.Diagram;
import org.reactome.server.tools.diagram.data.layout.impl.CoordinateFactory;

/**
 * Class used to rescale the diagram and all the elements present so then it will fit into the slide size. Taking
 * into account the rescaling is implemented because
 *
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */
public class Adjustment {

    private static final double MAX_HEIGHT = 4032;
    private static final double MAX_WIDTH = 4032;
    private static final int MARGIN = 15;
    private double factor = 1;
    private Double slideWidth;
    private Double slideHeight;
    private Coordinate panning;

    public Adjustment(Diagram diagram) {
        //Original boundaries coordinates
        Coordinate min = CoordinateFactory.get(diagram.getMinX(), diagram.getMinY());
        Coordinate max = CoordinateFactory.get(diagram.getMaxX(), diagram.getMaxY());

        //Panning adjustment to MARGIN
        panning = CoordinateFactory.get(MARGIN, MARGIN).minus(min);

        //Moving diagram to (MARGIN,MARGIN)
        min = min.minus(panning);
        max = max.minus(panning);

        Coordinate diagramSize = max.minus(min).add(CoordinateFactory.get(MARGIN * 2, MARGIN * 2));

        slideWidth = diagramSize.getX();
        slideHeight = diagramSize.getY();

        // size exceeded, calculate the factor
        if (slideWidth > MAX_WIDTH || slideHeight > MAX_HEIGHT) {
            //Calculating the appropriate factor
            double factorHeight = MAX_HEIGHT / slideHeight;
            double factorWidth = MAX_WIDTH / slideWidth;
            factor = factorHeight < factorWidth ? factorHeight : factorWidth;

            //Readjusting the slide size
            slideWidth = diagramSize.getX() * factor;
            slideHeight = diagramSize.getY() * factor;
        }

        //If the diagram didn't fit the slide, the coordinate also needs to adjust
        panning = panning.multiply(factor);
    }

    public double getFactor() {
        if (factor <= 0 || factor > 1){
            System.out.println("Something weird here, factor should be between 0 and 1: " + factor);
        }
        return factor;
    }

    public int getSlideHeight() {
        return slideHeight.intValue();
    }

    public int getSlideWidth() {
        return slideWidth.intValue();
    }

    public Coordinate getPanning() {
        return panning;
    }
}
