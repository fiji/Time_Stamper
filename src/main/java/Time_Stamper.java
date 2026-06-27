import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.DialogListener;
import ij.gui.RoiListener;
import ij.gui.Roi;
import ij.gui.Overlay;
import ij.gui.TextRoi;
import ij.gui.Toolbar;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;

import java.awt.AWTEvent;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.TextField;
import java.util.Vector;

public class Time_Stamper implements ExtendedPlugInFilter, DialogListener, RoiListener {
    ImagePlus imp;
    static int x = 10;
    static int y = 30;
    static int size = 12;
    Font font;
    static double start = 0;
    static double interval = 1;
    static String suffix = "sec";
    static int decimalPlaces = 0;
    int idx = 1;
    static boolean digital = false;
    boolean AAtext = true;
    boolean addToOverlay = true;
    static String fontName = "SansSerif";
    static String fontStyleStr = "Plain";
    static String colorStr = "White";
    static String justificationStr = "Left";
    static String customFormat = "";
    static String timeUnitStr = "Seconds";
    static boolean createNewWindow = false;

    // For preview and listener synchronization
    private TextRoi previewRoi;
    private GenericDialog gdInstance;
    private boolean isUpdatingRoi = false;

    final int flags = DOES_ALL + DOES_STACKS + STACK_REQUIRED;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        IJ.register(Time_Stamper.class);
        return flags;
    }

    public void setNPasses(int nPasses) {
        // nothing to do here
    }

    public void run(ImageProcessor ip) {
        Color selectedColor = getSelectedColor();
        ip.setFont(font);
        ip.setColor(selectedColor);
        ip.setJustification(0); // Always draw left-justified (0) and control position using rx
        ip.setAntialiasedText(AAtext);

        // use frames, if possible
        boolean useFrames = imp.isHyperStack() || imp.getNFrames() > 1;
        // get current time
        int[] pos = imp.convertIndexToPosition(idx);
        int frame = useFrames ? pos[2] : pos[1];
        double time = start + (frame - 1) * interval;
        
        // create output string
        String s = getFormattedText(time);

        if (addToOverlay) {
            Overlay overlay = imp.getOverlay();
            if (overlay == null) {
                overlay = new Overlay();
                imp.setOverlay(overlay);
            }
            // Create temporary TextRoi to measure actual bounds width, then calculate alignment
            TextRoi textRoi = new TextRoi(x, y, s, font);
            int w = textRoi.getBounds().width;
            int rx = x;
            if (justificationStr.equals("Center")) {
                rx = x - (w / 2);
            } else if (justificationStr.equals("Right")) {
                rx = x - w;
            }
            textRoi.setLocation(rx, y);
            textRoi.setStrokeColor(selectedColor);
            // Do not call setJustification, control alignment solely by left coordinate rx
            textRoi.setAntiAlias(AAtext);
            
            // Determine position setting method depending on whether it is a HyperStack or a standard Stack
            if (imp.isHyperStack()) {
                textRoi.setPosition(pos[0], pos[1], pos[2]);
            } else {
                textRoi.setPosition(idx);
            }
            overlay.add(textRoi);
        } else {
            // Calculate drawing start position (rx) based on alignment settings
            int w = ip.getStringWidth(s);
            int rx = x;
            if (justificationStr.equals("Center")) {
                rx = x - (w / 2);
            } else if (justificationStr.equals("Right")) {
                rx = x - w;
            }
            ip.moveTo(rx, y + size);
            ip.drawString(s);
        }
        // increment frame number
        idx++;
    }

    String getFormattedText(double time) {
        if (customFormat != null && !customFormat.trim().isEmpty()) {
            return formatCustomTime(time, customFormat);
        } else if (digital) {
            return getString2(time);
        } else {
            return getString(time);
        }
    }

    String formatCustomTime(double time, String format) {
        if (time < 0) time = 0;
        
        // Normalize time value to Hours
        double timeInHours = time;
        if (timeUnitStr.equals("Seconds")) {
            timeInHours = time / 3600.0;
        } else if (timeUnitStr.equals("Minutes")) {
            timeInHours = time / 60.0;
        }
        
        boolean containsD = format.contains("${d}") || format.contains("${dd}");
        boolean containsH = format.contains("${h}") || format.contains("${hh}");
        boolean containsM = format.contains("${m}") || format.contains("${mm}");
        boolean containsS = format.contains("${s}") || format.contains("${ss}");
        
        double rem = timeInHours; // Converted time value
        
        int days = 0;
        if (containsD) {
            if (!containsH && !containsM && !containsS) {
                days = (int)Math.round(rem / 24.0);
            } else {
                days = (int)(rem / 24.0);
            }
            rem = rem % 24.0;
        }
        
        int hours = 0;
        if (containsH) {
            if (!containsM && !containsS) {
                hours = (int)Math.round(rem);
            } else {
                hours = (int)rem;
            }
            rem = (rem - hours) * 60.0;
        } else {
            rem = rem * 60.0;
        }
        
        int minutes = 0;
        if (containsM) {
            if (!containsS) {
                minutes = (int)Math.round(rem);
            } else {
                minutes = (int)rem;
            }
            rem = (rem - minutes) * 60.0;
        } else {
            rem = rem * 60.0;
        }
        
        double seconds = rem;
        
        // Format seconds (apply Decimal Places)
        String sSec;
        if (decimalPlaces <= 0) {
            sSec = String.valueOf((int)Math.round(seconds));
        } else {
            sSec = IJ.d2s(seconds, decimalPlaces);
        }
        
        // Create 2-digit zero-padded seconds for ${ss}
        String ssSec = sSec;
        int dotIndex = sSec.indexOf('.');
        String intPart = dotIndex >= 0 ? sSec.substring(0, dotIndex) : sSec;
        String fracPart = dotIndex >= 0 ? sSec.substring(dotIndex) : "";
        if (intPart.length() < 2) {
            ssSec = "0" + intPart + fracPart;
        }
        
        // Replace placeholders
        String result = format;
        result = result.replace("${dd}", String.format("%02d", days));
        result = result.replace("${d}", String.valueOf(days));
        result = result.replace("${hh}", String.format("%02d", hours));
        result = result.replace("${h}", String.valueOf(hours));
        result = result.replace("${mm}", String.format("%02d", minutes));
        result = result.replace("${m}", String.valueOf(minutes));
        result = result.replace("${ss}", ssSec);
        result = result.replace("${s}", sSec);
        
        return result;
    }

    String getString(double time) {
        if (Math.abs(interval) < 0.00001)
            return suffix;
        // cut decimal places if they are not wanted
        if (decimalPlaces == 0)
            return (int)time + " " + suffix;
        else
            return IJ.d2s(time, decimalPlaces) + " " + suffix;
    }

    String getString2(double time) {
        if (time < 10) return "00:0"+(int)time;
        if (time < 60) return "00:"+(int)time;
        if (time >= 60) {
            int hour = (int) time/60;
            int min  = (int) time%60;
            if (hour < 10 && min < 10) return "0"+hour+":0"+min;
            if (hour < 10 && min >=10) return "0"+hour+":"+min;
            if (hour >= 10 && min < 10) return hour+":0"+min;
            if (hour >= 10 && min >=10) return hour+":"+min;
        }
        // cut decimal places if they are not wanted
        if (decimalPlaces==0)
            return "" + (int)time;
        else
            return IJ.d2s(time,decimalPlaces);
    }



    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        this.imp = imp;
        ImageProcessor ip = imp.getProcessor();
        Rectangle roi = ip.getRoi();
        
        // Initial position setup
        if (roi.width < ip.getWidth() || roi.height < ip.getHeight()) {
            x = roi.x;
            y = roi.y;
            size = (int) ((roi.height - 1.10526)/0.934211);
            if (size < 7) size = 7;
            if (size > 80) size = 80;
        }

        // Font setup for preview
        font = getSelectedFont(size);

        // Temporarily save original ROI
        Roi originalRoi = imp.getRoi();

        // Create preview ROI
        String previewText = getFormattedText(start);
        previewRoi = new TextRoi(x, y, previewText, font);
        int w = previewRoi.getBounds().width;
        int rx = x;
        if (justificationStr.equals("Center")) {
            rx = x - (w / 2);
        } else if (justificationStr.equals("Right")) {
            rx = x - w;
        }
        previewRoi.setLocation(rx, y);
        previewRoi.setStrokeColor(getSelectedColor());
        // Do not call setJustification
        imp.setRoi(previewRoi);

        // Create non-blocking generic dialog
        NonBlockingGenericDialog gd = new NonBlockingGenericDialog("Time Stamper (Customized)");
        gdInstance = gd;
        String[] fonts = {"SansSerif", "Serif", "Monospaced", "Arial"};
        String[] styles = {"Plain", "Bold", "Italic", "Bold+Italic"};
        String[] colors = {"White", "Black", "Red", "Green", "Blue", "Yellow", "Magenta", "Cyan", "Orange"};
        String[] alignments = {"Left", "Center", "Right"};
        String[] timeUnits = {"Seconds", "Minutes", "Hours"};

        gd.addNumericField("Starting Time:", start, 2);
        gd.addNumericField("Interval:", interval, 2);
        gd.addNumericField("Decimal Places:", decimalPlaces, 0);
        gd.addNumericField("X Location (left):", x, 0);
        gd.addNumericField("Y Location (top):", y, 0);
        gd.addNumericField("Font Size:", size, 0);
        gd.addChoice("Font Name:", fonts, fontName);
        gd.addChoice("Font Style:", styles, fontStyleStr);
        gd.addChoice("Color:", colors, colorStr);
        gd.addChoice("Justification:", alignments, justificationStr);

        gd.addCheckbox("'00:00' format:", digital);
        gd.addStringField("Or with a Suffix:", suffix);
        gd.addStringField("Or Custom Format:", customFormat, 20);
        gd.addMessage("        (e.g. ${d}d${hh}hr${mm}min${ss}sec)");
        gd.addChoice("Time Unit:", timeUnits, timeUnitStr);
        gd.addCheckbox("Anti-Aliased text?", AAtext);
        gd.addCheckbox("Overlay", addToOverlay);
        gd.addCheckbox("Create New Window", createNewWindow);
        
        gd.addDialogListener(this);
        Roi.addRoiListener(this);

        gd.showDialog();

        // Stop listening when dialog is closed
        Roi.removeRoiListener(this);
        gdInstance = null;

        if (gd.wasCanceled()) {
            // Remove preview and restore original ROI if canceled
            if (originalRoi != null && originalRoi != previewRoi) {
                imp.setRoi(originalRoi);
            } else {
                imp.killRoi();
            }
            return DONE;
        }

        // Confirm values
        start = gd.getNextNumber();
        interval = gd.getNextNumber();
        decimalPlaces = (int)gd.getNextNumber();
        x = (int)gd.getNextNumber();
        y = (int)gd.getNextNumber();
        size = (int)gd.getNextNumber();

        fontName = gd.getNextChoice();
        fontStyleStr = gd.getNextChoice();
        colorStr = gd.getNextChoice();
        justificationStr = gd.getNextChoice();

        digital = gd.getNextBoolean();
        suffix = gd.getNextString();
        customFormat = gd.getNextString();
        timeUnitStr = gd.getNextChoice();
        AAtext = gd.getNextBoolean();
        addToOverlay = gd.getNextBoolean();
        createNewWindow = gd.getNextBoolean();

        // Get the latest dragged position
        Roi finalRoi = imp.getRoi();
        if (finalRoi != null && finalRoi == previewRoi) {
            rx = finalRoi.getBounds().x; // Left edge of the moved text
            y = finalRoi.getBounds().y;

            // Calculate baseline x coordinate back from the left coordinate based on alignment
            w = finalRoi.getBounds().width;
            if (justificationStr.equals("Center")) {
                x = rx + (w / 2);
            } else if (justificationStr.equals("Right")) {
                x = rx + w;
            } else {
                x = rx;
            }
        }

        if (createNewWindow) {
            ImagePlus impCopy = imp.duplicate();
            impCopy.setTitle(imp.getTitle() + " - Timestamps");
            impCopy.show();
            
            // Temporarily redirect processor reference to the duplicate image
            ImagePlus originalImp = this.imp;
            this.imp = impCopy;
            
            int nSlices = impCopy.getStackSize();
            int originalIdx = idx;
            idx = 1;
            for (int i = 1; i <= nSlices; i++) {
                impCopy.setPosition(i);
                ImageProcessor ipCopy = impCopy.getProcessor();
                run(ipCopy);
            }
            idx = originalIdx;
            
            // Restore reference to the original image
            this.imp = originalImp;
            
            impCopy.updateAndDraw();
            
            // Clear preview on the original image
            imp.killRoi();
            imp.updateAndDraw();
            
            return DONE; // Stop PlugInFilterRunner from processing the original image
        }

        font = getSelectedFont(size);
        ip.setFont(font);

        // Clear preview ROI
        imp.killRoi();

        imp.startTiming();
        return flags;
    }

    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        double pStart = gd.getNextNumber();
        double pInterval = gd.getNextNumber();
        int pDecimalPlaces = (int)gd.getNextNumber();
        int pX = (int)gd.getNextNumber();
        int pY = (int)gd.getNextNumber();
        int pSize = (int)gd.getNextNumber();
        
        String pFontName = gd.getNextChoice();
        String pFontStyleStr = gd.getNextChoice();
        String pColorStr = gd.getNextChoice();
        String pJustificationStr = gd.getNextChoice();

        boolean pDigital = gd.getNextBoolean();
        String pSuffix = gd.getNextString();
        String pCustomFormat = gd.getNextString();
        String pTimeUnit = gd.getNextChoice();
        boolean pAAtext = gd.getNextBoolean();
        boolean pAddToOverlay = gd.getNextBoolean();
        boolean pCreateNewWindow = gd.getNextBoolean();

        if (gd.invalidNumber()) {
            return false;
        }

        start = pStart;
        interval = pInterval;
        decimalPlaces = pDecimalPlaces;
        x = pX;
        y = pY;
        size = pSize;
        fontName = pFontName;
        fontStyleStr = pFontStyleStr;
        colorStr = pColorStr;
        justificationStr = pJustificationStr;
        digital = pDigital;
        suffix = pSuffix;
        customFormat = pCustomFormat;
        timeUnitStr = pTimeUnit;
        AAtext = pAAtext;
        addToOverlay = pAddToOverlay;
        createNewWindow = pCreateNewWindow;

        // Update preview
        if (previewRoi != null) {
            font = getSelectedFont(size);
            String previewText = getFormattedText(start);
            previewRoi.setText(previewText);
            previewRoi.setFont(font);
            previewRoi.setStrokeColor(getSelectedColor());

            int w = previewRoi.getBounds().width;
            int rx = x;
            if (justificationStr.equals("Center")) {
                rx = x - (w / 2);
            } else if (justificationStr.equals("Right")) {
                rx = x - w;
            }
            previewRoi.setLocation(rx, y);
            imp.updateAndDraw();
        }

        return true;
    }

    int getTextWidth(String text, Font font) {
        if (imp == null) return 0;
        ImageProcessor ip = imp.getProcessor();
        Font oldFont = ip.getFont();
        ip.setFont(font);
        int w = ip.getStringWidth(text);
        ip.setFont(oldFont);
        return w;
    }

    Font getSelectedFont(int size) {
        int style = Font.PLAIN;
        if (fontStyleStr.equals("Bold")) {
            style = Font.BOLD;
        } else if (fontStyleStr.equals("Italic")) {
            style = Font.ITALIC;
        } else if (fontStyleStr.equals("Bold+Italic")) {
            style = Font.BOLD | Font.ITALIC;
        }
        return new Font(fontName, style, size);
    }

    Color getSelectedColor() {
        if (colorStr.equals("Black")) return Color.black;
        if (colorStr.equals("Red")) return Color.red;
        if (colorStr.equals("Green")) return Color.green;
        if (colorStr.equals("Blue")) return Color.blue;
        if (colorStr.equals("Yellow")) return Color.yellow;
        if (colorStr.equals("Magenta")) return Color.magenta;
        if (colorStr.equals("Cyan")) return Color.cyan;
        if (colorStr.equals("Orange")) return Color.orange;
        return Color.white;
    }

    int getJustification() {
        if (justificationStr.equals("Center")) {
            return 1; // TextRoi.CENTER / ImageProcessor.CENTER_JUSTIFY
        } else if (justificationStr.equals("Right")) {
            return 2; // TextRoi.RIGHT / ImageProcessor.RIGHT_JUSTIFY
        }
        return 0; // TextRoi.LEFT / ImageProcessor.LEFT_JUSTIFY
    }

    public void roiModified(ImagePlus imp, int id) {
        if (isUpdatingRoi) return;
        if (imp != this.imp || previewRoi == null) return;

        Roi roi = imp.getRoi();

        if (roi != null) {
            if (roi != previewRoi) {
                // When user clicks a different location on the image (new ROI created)
                Rectangle r = roi.getBounds();
                int click_x = r.x;
                y = r.y;

                // Use the clicked location as the alignment baseline
                x = click_x;

                // Update coordinates in the dialog
                updateDialogFields();

                // Get actual width of the preview TextRoi
                int w = previewRoi.getBounds().width;

                // Calculate start coordinate (rx) based on alignment
                int rx = x;
                if (justificationStr.equals("Center")) {
                    rx = x - (w / 2);
                } else if (justificationStr.equals("Right")) {
                    rx = x - w;
                }

                // Move preview ROI to the clicked position and redraw image
                isUpdatingRoi = true;
                previewRoi.setLocation(rx, y);
                imp.setRoi(previewRoi);
                isUpdatingRoi = false;


            } else {
                // When the preview ROI itself is dragged
                if (id == RoiListener.MOVED || id == RoiListener.MODIFIED) {
                    Rectangle r = roi.getBounds();
                    int rx = r.x; // Left edge of the moved text
                    y = r.y;

                    // Get actual width of the preview TextRoi
                    int w = previewRoi.getBounds().width;

                    // Calculate baseline x coordinate back from the left coordinate based on alignment
                    if (justificationStr.equals("Center")) {
                        x = rx + (w / 2);
                    } else if (justificationStr.equals("Right")) {
                        x = rx + w;
                    } else {
                        x = rx;
                    }

                    updateDialogFields();
                }
            }
        }
    }

    private void updateDialogFields() {
        if (gdInstance != null) {
            Vector nFields = gdInstance.getNumericFields();
            if (nFields != null && nFields.size() > 4) {
                TextField xField = (TextField) nFields.get(3);
                TextField yField = (TextField) nFields.get(4);
                if (xField != null && !xField.isFocusOwner()) {
                    xField.setText(String.valueOf(x));
                }
                if (yField != null && !yField.isFocusOwner()) {
                    yField.setText(String.valueOf(y));
                }
            }
        }
    }
}
