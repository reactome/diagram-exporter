package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.*;
import org.reactome.server.tools.diagram.data.layout.Coordinate;
import org.reactome.server.tools.diagram.data.layout.NodeProperties;
import org.reactome.server.tools.diagram.data.layout.Note;
import org.reactome.server.tools.diagram.data.layout.impl.NodePropertiesFactory;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfile;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */
public class CompartmentNote {
	private static final String PROFILE_TYPE = "note";
	private float width = 1;
	private float height = 1;
	private String displayName;
	private Coordinate textPosition;
	private Stylesheet stylesheet;
	private Adjustment adjustment;

	public CompartmentNote(Note note, DiagramProfile profile, Adjustment adjustment) {
		NodeProperties nodeProperties = NodePropertiesFactory.transform(note.getProp(), adjustment.getFactor(), adjustment.getPanning());
		this.width = nodeProperties.getWidth().floatValue();
		this.height = nodeProperties.getHeight().floatValue();
		this.displayName = note.getDisplayName();
		this.textPosition = note.getTextPosition();
		this.stylesheet = new Stylesheet(profile, PROFILE_TYPE);
		this.adjustment = adjustment;
	}

	public void render(IShapeCollection shapes) {
		NodeProperties np = NodePropertiesFactory.transform(NodePropertiesFactory.get(textPosition.getX().floatValue(), textPosition.getY().floatValue(), width, height), adjustment.getFactor(), adjustment.getPanning());
		IAutoShape iAutoShape = shapes.addAutoShape(ShapeType.Rectangle, np.getX().floatValue(), np.getY().floatValue(), np.getWidth().floatValue(), np.getHeight().floatValue());
		iAutoShape.setName(displayName);
		iAutoShape.getLineFormat().getFillFormat().setFillType(FillType.NoFill);
		iAutoShape.getFillFormat().setFillType(FillType.NoFill);
		iAutoShape.addTextFrame(" ");
		ITextFrame txtFrame = iAutoShape.getTextFrame();
		IParagraph para = txtFrame.getParagraphs().get_Item(0);
		IPortion portion = para.getPortions().get_Item(0);
		portion.getPortionFormat().getFillFormat().setFillType(FillType.Solid);
		portion.getPortionFormat().getFillFormat().getSolidFillColor().setColor(stylesheet.getTextColor());
		portion.getPortionFormat().setFontHeight(12);
		portion.getPortionFormat().setFontBold(NullableBool.True);
		portion.setText(displayName);
	}
}
