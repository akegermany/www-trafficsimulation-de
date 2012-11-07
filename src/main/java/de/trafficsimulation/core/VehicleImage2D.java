package de.trafficsimulation.core;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Draw a 2D vehicle on the screen
 * 
 * Typically, one constructs an image with VehicleImage2D(length_m, width_m, vehType,
 * isACC, color, scale),
 * 
 * sets its state variables:
 * setScale(scale) [optional]
 * setPosition(x_m, y_m)
 * setSpeed(speed_ms) [optional]
 * setDirection(phi) [optional]
 * 
 * and draws it:
 * draw(g, colorCode)
 * colorCode=0:physical color; 1:speed colormap; 2:physical color if ACC, gray otherwise
 * 
 * For labelling purposes, one can also set the pixel coordinates directly:
 * setPositionPix(xPix, yPix)
 * 
 * 
 * Units of variables with names *_m in meter, ALL others in pixels!
 * 
 * Physical units: x_m=0 <=> left,
 * y_m=0 <=> bottom,
 * 
 * pixel units: x=0 <=> left corner of drawing region,
 * y=0 <=> top (!) corner of drawing region.
 */

public class VehicleImage2D implements Constants {

    static final boolean DRAWBLACKLINES = true;
    static final boolean DRAW_MOREBLACKLINES = true;
    static final double SPEEDMIN_MS = 0;
    static final double SPEEDMAX_MS = 30;
    static final Color colorProbeveh = new Color(40, 0, 255);
    // color of perturbed vehicle in MicroStreet.java

    // physical vehicle properties

    double length_m;
    double width_m;
    String vehType; // {"car", "truck", "obstacle"}
    boolean isTruck;
    boolean isObstacle;
    boolean isACC;
    boolean isProbeveh;
    boolean isPerturbedveh;
    Color vehColor;

    // optical vehicle properties

    double scale; // pixel units per meter
    int colorCode; // 0=physical color; 1=speed colormap; 2=ACC colored, others gray

    // state variables

    double posx_m, posy_m; // position of vehicle center in physical coordinate system
    int posx, posy; // pixel units
    double phi; // phi=0: in x direction (moving to the right) phi=pi/2: in y (upwards)
    double speed_ms;

    // theta=90 => view from vertically above (z axis)
    // theta=0 => view from horizontal in direction of y axis
    // NOT YET IMPLEMETED!
    double theta;

    public VehicleImage2D(double length_m, double width_m, String vehType, boolean isACC, Color vehColor, double scale) {
        this.length_m = length_m;
        this.width_m = width_m;
        this.scale = scale;
        this.vehType = vehType;
        isTruck = (vehType.equals("truck"));
        isObstacle = (vehType.equals("obstacle"));
        this.isACC = isACC;
        this.vehColor = vehColor;
        speed_ms = 0;
        phi = 0;
        isProbeveh = false;
        isPerturbedveh = (vehColor.equals(MicroStreet.colorPerturb));
    }

    static int myround(double x) {
        return (int) Math.floor(x + 0.5);
    }

    private int getx(double x_m) {
        return (myround(scale * x_m));
    }

    private int gety(double y_m) {
        return (myround(-scale * y_m));
    }

    private int getdx(double dx_m) {
        return (myround(scale * dx_m));
    }

    private int getdy(double dy_m) {
        return (myround(-scale * dy_m));
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setPosition(double posx_m, double posy_m) {
        this.posx_m = posx_m;
        this.posy_m = posy_m;
        posx = getx(posx_m);
        posy = gety(posy_m);

    }

    public void setPositionPix(int posx_pix, int posy_pix) {
        this.posx = posx_pix;
        this.posy = posy_pix;
    }

    public void setSpeed(double speed) {
        this.speed_ms = speed;
    }

    public void setDirection(double phi) {
        this.phi = phi;
    }

    public void setProbeveh() {
        this.isProbeveh = true;
    }

    public void setPerturbedveh() {
        this.isPerturbedveh = true;
    }

    // not yet implemented
    public void setVerticalAngle(double theta) {
        this.theta = theta;
    }

    /**
     * hue values (see, e.g.,
     * http://help.adobe.com/en_US/Photoshop/11.0/images/wc_HSB.png):
     * h=0:red, h=0.2: yellow, h=0.35: green,
     * h=0.5: blue, h=0.65: violet, then a long magenta region
     */

    public static final Color colormapSpeed(double v, double vmin, double vmax) {

        // tune following values if not satisfied
        // (the floor function of any hue value >=1 will be subtracted by HSBtoRGB)

        float hue_vmin = (float) (0.95); // hue value for minimum speed value
        float hue_vmax = (float) (1.65); // hue value for max speed (1 will be subtracted)

        // possibly a nonlinear hue(speed) function looks nicer;
        // first try this truncuated-linear one

        float vRelative = (vmax > vmin) ? (float) ((v - vmin) / (vmax - vmin)) : 0;
        vRelative = Math.min(Math.max(0, vRelative), 1);
        // float h=hue_vmin+vRelative*(hue_vmax-hue_vmin);
        float vRelativeNonlin = vRelative
                + (float) (0.1 * (Math.cos(2 * Math.PI * (vRelative + 0.35)) - Math.cos(2 * Math.PI * (0.35))));
        float h = hue_vmin + vRelativeNonlin * (hue_vmax - hue_vmin);

        // use max. saturation
        float s = (float) (1.0);

        // possibly a reduction of brightness near h=0.5 looks nicer;
        // first try max brightness
        float b = (float) (1.0);

        return new Color(Color.HSBtoRGB(h, s, b));
    }

    public void draw(Graphics g, int colorCode) {

        this.colorCode = colorCode;

        // System.out.println("VehicleImage2D.draw: " + " posx = "
        // + posx + " posy = " + posy + " length_m = "
        // + length_m);

        // #########################################################
        // Design interface: all relative to center at
        // (posx_m,posy_m) and phi=0 (driving in pos x direction)
        // #########################################################

        // projection of front and back windows and roof

        // vehType.equals("car")
        double xFrontWindow2_m = 0.25 * length_m; // 0.20 (var 0.20, LKW)
        double xFrontWindow1_m = 0.05 * length_m; // 0.05 (var 0.05, LKW)
        double xBackWindow1_m = -0.22 * length_m; // -0.22 (var-0.35, LKW)
        double xBackWindow2_m = -0.40 * length_m; // -0.35 (var-0.48, LKW-0.50)
        double yRoofLeft_m = 0.40 * width_m; // 0.40 (LKW 0.50)

        if (vehType.equals("truck")) {
            xFrontWindow2_m = 0.30 * length_m; // 0.20 (var 0.20, LKW)
            xFrontWindow1_m = 0.25 * length_m; // 0.05 (var 0.05, LKW)
            xBackWindow1_m = -0.48 * length_m; // -0.22 (var-0.35, LKW)
            xBackWindow2_m = -0.49 * length_m; // -0.35 (var-0.48, LKW-0.50)
            yRoofLeft_m = 0.50 * width_m; // 0.40 (LKW 0.50)
        }

        // left "hood line"

        double xHood1_m = xFrontWindow2_m + 0.05 * length_m;
        double xHood2_m = 0.45 * length_m;
        double yHood1_m = 0.35 * width_m;
        double yHood2_m = 0.25 * width_m;

        // left mirror (front side of mirror attached at xFrontWindow2_m)

        double xMirr1_m = xFrontWindow2_m - 0.04 * length_m; // tip of mirror
        double xMirr2_m = xFrontWindow2_m - 0.03 * length_m; // "mirror" side
        double yMirr1_m = 0.60 * width_m;
        double yMirr2_m = 0.50 * width_m;

        // #########################################################
        // end "design interface"
        // #########################################################

        // rel coord vehicle at posx=posy=phi=0 (driving in pos. x direction)

        // vehicle outline

        double halflen_m = 0.5 * length_m;
        double halfw_m = 0.5 * width_m;
        double xCorners_m[] = { -halflen_m, -halflen_m, halflen_m, halflen_m };
        double yCorners_m[] = { halfw_m, -halfw_m, -halfw_m, halfw_m };

        // front and back windows

        double xFrontw_m[] = { xFrontWindow1_m, xFrontWindow1_m, xFrontWindow2_m, xFrontWindow2_m };
        double yFrontw_m[] = { yRoofLeft_m, -yRoofLeft_m, -halfw_m, halfw_m };

        double xBackw_m[] = { xBackWindow1_m, xBackWindow1_m, xBackWindow2_m, xBackWindow2_m };
        double yBackw_m[] = { yRoofLeft_m, -yRoofLeft_m, -halfw_m, halfw_m };

        // roof

        double xRoof_m[] = { xBackWindow1_m, xBackWindow1_m, xFrontWindow1_m, xFrontWindow1_m };
        double yRoof_m[] = { yRoofLeft_m, -yRoofLeft_m, -yRoofLeft_m, yRoofLeft_m };

        // hood

        double xHood_m[] = { xHood1_m, xHood2_m, xHood1_m, xHood2_m };
        double yHood_m[] = { yHood1_m, yHood2_m, -yHood1_m, -yHood2_m };

        // mirrors

        double xMirrL_m[] = { xFrontWindow2_m, xMirr1_m, xMirr2_m };
        double yMirrL_m[] = { 0.50 * width_m, yMirr1_m, yMirr2_m };
        double xMirrR_m[] = { xFrontWindow2_m, xMirr1_m, xMirr2_m };
        double yMirrR_m[] = { -0.50 * width_m, -yMirr1_m, -yMirr2_m };

        // ####################################
        // draw: rotate + scale to pixels
        // ####################################

        double cp = Math.cos(-phi); // negative angle since yPix direction downwards
        double sp = Math.sin(-phi);

        int xCorners[] = new int[4];
        int yCorners[] = new int[4];
        int xFrontw[] = new int[4];
        int yFrontw[] = new int[4];
        int xBackw[] = new int[4];
        int yBackw[] = new int[4];
        int xRoof[] = new int[4];
        int yRoof[] = new int[4];
        int xHood[] = new int[4];
        int yHood[] = new int[4];
        int xMirrL[] = new int[3];
        int yMirrL[] = new int[3];
        int xMirrR[] = new int[3];
        int yMirrR[] = new int[3];

        for (int i = 0; i < 4; i++) {
            xCorners[i] = posx + getdx(cp * xCorners_m[i] - sp * yCorners_m[i]);
            yCorners[i] = posy + getdy(sp * xCorners_m[i] + cp * yCorners_m[i]);

            xFrontw[i] = posx + getdx(cp * xFrontw_m[i] - sp * yFrontw_m[i]);
            yFrontw[i] = posy + getdy(sp * xFrontw_m[i] + cp * yFrontw_m[i]);

            xBackw[i] = posx + getdx(cp * xBackw_m[i] - sp * yBackw_m[i]);
            yBackw[i] = posy + getdy(sp * xBackw_m[i] + cp * yBackw_m[i]);

            xRoof[i] = posx + getdx(cp * xRoof_m[i] - sp * yRoof_m[i]);
            yRoof[i] = posy + getdy(sp * xRoof_m[i] + cp * yRoof_m[i]);
            xHood[i] = posx + getdx(cp * xHood_m[i] - sp * yHood_m[i]);
            yHood[i] = posy + getdy(sp * xHood_m[i] + cp * yHood_m[i]);
        }

        for (int i = 0; i < 3; i++) {
            xMirrL[i] = posx + getdx(cp * xMirrL_m[i] - sp * yMirrL_m[i]);
            yMirrL[i] = posy + getdy(sp * xMirrL_m[i] + cp * yMirrL_m[i]);
            xMirrR[i] = posx + getdx(cp * xMirrR_m[i] - sp * yMirrR_m[i]);
            yMirrR[i] = posy + getdy(sp * xMirrR_m[i] + cp * yMirrR_m[i]);
        }

        // #########################################
        // draw: actually paint the pixels
        // #########################################

        Color drawColor = (isObstacle) ? Color.black : ((colorCode == 0) || ((colorCode == 1) && isTruck)) ? vehColor
                : (colorCode == 1) ? colormapSpeed(speed_ms, SPEEDMIN_MS, SPEEDMAX_MS) : (isACC) ? vehColor
                        : new Color(127, 127, 127);
        if (isPerturbedveh) {
            drawColor = vehColor;
        } // info "isPerturbed" by vehColor
        if (isProbeveh) {
            drawColor = colorProbeveh;
        }
        // System.out.println("VehicleImage2D: drawColor="+drawColor);
        // System.out.println("scale="+scale);
        // System.out.println("xCorners="+xCorners[0]+" "+xCorners[1]+" "+xCorners[2]+" "+xCorners[3]);
        g.setColor(drawColor);
        g.fillPolygon(xCorners, yCorners, 4);
        if (!isObstacle) {
            g.setColor(drawColor.brighter().brighter());
            g.fillPolygon(xFrontw, yFrontw, 4);
            g.setColor(drawColor.darker().darker());
            g.fillPolygon(xBackw, yBackw, 4);

            int w = getdx(width_m);
            boolean draw_blacklines = (DRAWBLACKLINES && (w > 3)); // outline
            boolean draw_more_blacklines = (DRAW_MOREBLACKLINES && (w > 8)); // hood etc
            boolean draw_mirrors = false;

            if (draw_blacklines) {
                g.setColor(Color.black);
                g.drawPolygon(xCorners, yCorners, 4);
                if (draw_more_blacklines) {
                    g.drawPolygon(xFrontw, yFrontw, 4);
                    g.drawPolygon(xBackw, yBackw, 4);
                    g.drawPolygon(xRoof, yRoof, 4);
                    g.drawLine(xHood[0], yHood[0], xHood[1], yHood[1]);
                    g.drawLine(xHood[2], yHood[2], xHood[3], yHood[3]);
                    if (draw_mirrors) {
                        g.drawPolygon(xMirrL, yMirrL, 3);
                        g.drawPolygon(xMirrR, yMirrR, 3);
                    }
                }
            }
        } // !isObstacle
    } // end draw(..)

}// VehicleImage3D
