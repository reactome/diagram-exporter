package org.reactome.server.tools.diagram.exporter.pptx.model;

import com.aspose.slides.LineArrowheadLength;
import com.aspose.slides.LineArrowheadStyle;
import com.aspose.slides.LineArrowheadWidth;
import com.aspose.slides.LineCapStyle;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfile;
import org.reactome.server.tools.diagram.data.profile.diagram.DiagramProfileNode;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Guilherme S Viteri (gviteri@ebi.ac.uk)
 */

public class Stylesheet {

    private double lineWidth;
    private Color lineColor;
    private Color fillColor;
    private Color textColor;
    private Color fadeOutStroke;
    private Color fadeOutFill;
    private Color fadeOutText;
    private Color lighterStroke;
    private Color lighterFill;
    private Color lighterText;
    private byte shapeFillType;
    private byte lineFillType;
    private byte lineStyle;
    private byte lineDashStyle;
    private byte lineCapStyle = LineCapStyle.Round;

    // arrow
    private byte lineArrowheadLength = LineArrowheadLength.Long;
    private byte lineArrowheadStyle = LineArrowheadStyle.Triangle;
    private byte lineArrowheadWidth = LineArrowheadWidth.Wide;

    // decorators
    private Color diseaseColor;
    private Color flagColor;
    private Color selectionColor;
    private double selectionLineWidth = 3;

    // font
    private int fontHeight;

    public Stylesheet() {

    }

    public Stylesheet(DiagramProfile profile, String type, byte shapeFillType, byte lineFillType, byte lineStyle, int fontHeight) {
        DiagramProfileNode profileInfo = getProfileNode(profile, type);
        this.lineWidth = profileInfo.getLineWidth() != null ? Double.valueOf(profileInfo.getLineWidth()) * 2 : 1;
        this.lineColor = parseColor(profileInfo.getStroke());
        this.fillColor = parseColor(profileInfo.getFill());
        this.textColor = parseColor(profileInfo.getText());
        this.fadeOutStroke = parseColor(profileInfo.getFadeOutStroke());
        this.fadeOutFill = parseColor(profileInfo.getFadeOutFill());
        this.fadeOutText = parseColor(profileInfo.getFadeOutText());
        this.lighterStroke = parseColor(profileInfo.getLighterStroke());
        this.lighterFill = parseColor(profileInfo.getLighterFill());
        this.lighterText = parseColor(profileInfo.getLighterText());
        this.shapeFillType = shapeFillType;
        this.lineFillType = lineFillType;
        this.lineStyle = lineStyle;
        this.diseaseColor = parseColor(profile.getProperties().getDisease());
        this.flagColor = parseColor(profile.getProperties().getFlag());
        this.selectionColor = parseColor(profile.getProperties().getSelection());
        this.fontHeight = fontHeight;
    }

    public Stylesheet(DiagramProfile profile, String type, byte shapeFillType, byte lineFillType, byte lineStyle) {
        this(profile, type, shapeFillType, lineFillType, lineStyle, 10);
    }

    public Stylesheet(DiagramProfile profile, String type) {
        this(profile, type, (byte) 0, (byte) 0, (byte) 0);
    }

    private static Color parseColor(String color) {
        if (color == null) return null;

        if (color.startsWith("#")) {
            return hexToColor(color);
        }
        return rgbaToColor(color);
    }

    private static Color hexToColor(String input) {
        int r = Integer.valueOf(input.substring(1, 3), 16);
        int g = Integer.valueOf(input.substring(3, 5), 16);
        int b = Integer.valueOf(input.substring(5, 7), 16);

        return new Color(r, g, b);
    }

    private static Color rgbaToColor(String input) {
        String rgbaRegex = "^rgba\\(\\s*(0|[1-9]\\d?|1\\d\\d?|2[0-4]\\d|25[0-5])\\s*,\\s*(0|[1-9]\\d?|1\\d\\d?|2[0-4]\\d|25[0-5])\\s*,\\s*(0|[1-9]\\d?|1\\d\\d?|2[0-4]\\d|25[0-5])\\s*,\\s*((0.[1-9])|[01])\\s*\\)$";

        Pattern c = Pattern.compile(rgbaRegex);
        Matcher m = c.matcher(input);
        if (m.matches()) {
            return new Color(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), (int) (Float.parseFloat(m.group(4)) * 255f));
        }
        return null;
    }

    /**
     * Apply custom style.
     * Mainly used in the connector or in the auxiliary shapes.
     */
    public Stylesheet customStyle(double lineWidth, byte lineStyle, byte lineFillType, Color lineColor, byte shapeFillType, Color fillColor, byte lineDashStyle, byte lineCapStyle) {
        this.setLineWidth(lineWidth);
        this.setLineStyle(lineStyle);
        this.setLineFillType(lineFillType);
        this.setLineColor(lineColor);
        this.setShapeFillType(shapeFillType);
        this.setFillColor(fillColor);
        this.setLineDashStyle(lineDashStyle);
        this.setLineCapStyle(lineCapStyle);
        return this;
    }

    /**
     * Apply custom style.
     * Mainly used in the connector or in the auxiliary shapes.
     */
    public Stylesheet customStyle(double lineWidth, byte lineStyle, byte lineFillType, Color lineColor, byte shapeFillType, Color fillColor, byte lineDashStyle) {
        return customStyle(lineWidth, lineStyle, lineFillType, lineColor, shapeFillType, fillColor, lineDashStyle, this.lineCapStyle);
    }


    public Color getFadeOutFill() {
        return fadeOutFill;
    }

    public Color getFadeOutStroke() {
        return fadeOutStroke;
    }

    public Color getFadeOutText() {
        return fadeOutText;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public Color getLighterFill() {
        return lighterFill;
    }

    public Color getLighterStroke() {
        return lighterStroke;
    }

    public Color getLighterText() {
        return lighterText;
    }

    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }

    public byte getLineFillType() {
        return lineFillType;
    }

    public void setLineFillType(byte lineFillType) {
        this.lineFillType = lineFillType;
    }

    public byte getLineStyle() {
        return lineStyle;
    }

    public void setLineStyle(byte lineStyle) {
        this.lineStyle = lineStyle;
    }

    public double getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
    }

    public byte getShapeFillType() {
        return shapeFillType;
    }

    public void setShapeFillType(byte shapeFillType) {
        this.shapeFillType = shapeFillType;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    public byte getLineDashStyle() {
        return lineDashStyle;
    }

    public void setLineDashStyle(byte lineDashStyle) {
        this.lineDashStyle = lineDashStyle;
    }

    public byte getLineArrowheadLength() {
        return lineArrowheadLength;
    }

    public void setLineArrowheadLength(byte lineArrowheadLength) {
        this.lineArrowheadLength = lineArrowheadLength;
    }

    public byte getLineArrowheadStyle() {
        return lineArrowheadStyle;
    }

    public void setLineArrowheadStyle(byte lineArrowheadStyle) {
        this.lineArrowheadStyle = lineArrowheadStyle;
    }

    public byte getLineArrowheadWidth() {
        return lineArrowheadWidth;
    }

    public void setLineArrowheadWidth(byte lineArrowheadWidth) {
        this.lineArrowheadWidth = lineArrowheadWidth;
    }

    public byte getLineCapStyle() {
        return lineCapStyle;
    }

    public void setLineCapStyle(byte lineCapStyle) {
        this.lineCapStyle = lineCapStyle;
    }

    public Color getDiseaseColor() {
        return diseaseColor;
    }

    public Color getFlagColor() {
        return flagColor;
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    public double getSelectionLineWidth() {
        return selectionLineWidth;
    }

    public int getFontHeight() {
        return fontHeight;
    }

    public void setFontHeight(int fontHeight) {
        this.fontHeight = fontHeight;
    }

    private DiagramProfileNode getProfileNode(DiagramProfile profile, String type) {
        switch (type.toLowerCase()) {
            case "attachment":
                return profile.getAttachment();
            case "cell":
                return profile.getCell();
            case "cell-nucleus":
                return profile.getCellNucleus();
            case "chemical":
                return profile.getChemical();
            case "compartment":
                return profile.getCompartment();
            case "complex":
                return profile.getComplex();
            case "entity":
                return profile.getEntity();
            case "entityset":
                return profile.getEntityset();
            case "flowline":
                return profile.getFlowline();
            case "gene":
                return profile.getGene();
            case "interactor":
                return profile.getInteractor();
            case "link":
                return profile.getLink();
            case "note":
                return profile.getNote();
            case "otherentity":
                return profile.getOtherentity();
            case "processnode":
                return profile.getProcessnode();
            case "protein":
                return profile.getProtein();
            case "reaction":
                return profile.getReaction();
            case "rna":
                return profile.getRna();
            case "stoichiometry":
                return profile.getStoichiometry();
            case "encapsulatednode":
                return profile.getEncapsulatednode();
            case "chemicaldrug":
                return profile.getChemicaldrug();
            case "proteindrug":
                return profile.getProteindrug();
            case "rnadrug":
                return profile.getRnadrug();
            case "entitysetdrug":
                return profile.getEntitysetdrug();
            case "complexdrug":
                return profile.getComplexdrug();
            default:
                throw new IllegalArgumentException("Type " + type + " is not found in the JSON Profile.");
        }
    }
}
