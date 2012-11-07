package de.trafficsimulation.core;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Vector;

// !!! Units of variables with names *_m in meter, ALL others in pixels!

// !!! pixel units: x: left->right; y: top->bottom!!
// !!! offset in x: xstart: pixel units = xstart + scale*m-units

public class SimCanvas extends Canvas implements Runnable, Constants {

    static final int VEH_COLORCODE = 1; // 0: own colors; 1: speed colorcode; 2: VLA code

    // Scaling and global size parameters
    // (determine with width() method AFTER cstr)

    private static final long serialVersionUID = 1L;
    private int xsize; // width, height of window in pixels
    private int ysize; // y pixel region of circle or U w/o on-ramp
    private double scale; // scale factor pixels/m
    private double scalex;
    private double scaley;

    // position and size of various road elements
    // (_m vars in m; others in pixels)

    private double rdlen_m;
    private double rdlen; // (not rounded) road length in pixel units
    private double l_straight_m; // length(m) of straight sections of Ushape
    private int l_straight;
    private double total_rmplen_m; // length of onramp + visible access road
    private int total_rmplen;
    private double ramppos_m; // center ramp position (along road length)
    private int ramppos;

    private int lRamp;
    private int lw; // lane width
    private int r1, r2; // inner and outer radius
    private int radius; // radius of road center
    private int roadMargin; // space road-margin
    private int xc, yc; // center of SimCanvas (for all Szen)
    private int vehWidth; // in pixels
    private int pkwLen; // in pixels
    private int lkwLen; // in pixels

    // control variables

    private static int fcnr = 0; // index of floating car; all
                                 // Moveable objects with
                                 // index=0 painted in same color (blue)

    // parameters influenced by the user; updated from the
    // corresponding variables of MicroSim* by method newValues(..)

    private int choice_Szen = CHOICE_SCEN_INIT; // {stopGo+free,obstacles,..,..,sync}
    private double density;
    private double qIn;
    private double perTr;
    private double v0_limit;
    private double v0_limit_old = 0;
    private double p_factor;
    private double qRamp;
    private double p_factorRamp;
    private double deltaB;
    private double deltaBRamp;
    private int choice_Geom; // {Circle, U}

    // times

    private double time = 0.; // Simulation time (reset to 0 in each "run")
    private int itime = 0; // Accumulated simul. tinesteps
    private int itSinceLastChange = 0; // if displayed window changed
    // (itSinceLastChange==0) paint road grass etc anow, not only cars
    private int tsleep_ms;
    private int tsleepFrame_ms;
    private int simTime_ms;
    private int prepTime_ms;
    private int paintTime_ms;
    private int avgPaintTime_ms = 0;
    private int totalPaintTime_ms;
    private int totalTimeThisStep_ms;

    // Dynamical variables Array implementation
    // !!! rasius in m, dens_max in invkm; therefore 0.001!!!

    int imaxinit = (int) (0.001 * RADIUS_M * 4 * Math.PI * DENS_MAX_INVKM + 300);
    // initial array size; increase if necessary (!!! yet to do!!!)

    double[] pos = new double[imaxinit + 1];
    double[] speed = new double[imaxinit + 1];
    double[] lane = new double[imaxinit + 1];
    double[] lengthVeh = new double[imaxinit + 1];
    int[] nr = new int[imaxinit + 1];
    int[] old_i = new int[imaxinit + 1]; // such that nr[i] = old_nr[old_i]
    double[] old_pos = new double[imaxinit + 1];

    double[] posOn = new double[imaxinit + 1];
    double[] speedOn = new double[imaxinit + 1];
    double[] lengthVehOn = new double[imaxinit + 1];

    // main objects

    private Object microstreetLock = new Object();
    private MicroStreet microstreet;
    private OnRamp onramp;

    private Image buffer;
    private Image diagram1, diagram2, diagram3, diagram4;

    // when changing SIM_BG_COLOR, also change it in ../../MicroSim.java
    final Color SIM_BG_COLOR = new Color(20, 160, 0); // green grass
    // final Color SIM_BG_COLOR=new Color(180,255,150); // green grass

    private Color roadColor = Color.lightGray;

    private Graphics bufferedGraphics = null;
    private Thread runner = null;

    private int textHeight;
    private Font textFont;
    private Font textFontSmall;
    private FontMetrics metricsText;

    private Language lang;

    // Inset diagrams

    private int dia1x, dia2x, dia3x, dia4x, dia1y, dia2y, dia3y, dia4y;
    private int diaWidth, diaHeight;
    private TimeAvDet det1_0;
    private TimeAvDet det1_1;
    private SpaceAvDet det2_0;
    private SpaceAvDet det2_1;
    private TimeAvDet det3_0;
    private TimeAvDet det3_1;
    private SpaceAvDet det4_0;
    private SpaceAvDet det4_1;

    private TimeAvDet det5_0;
    private TimeAvDet det5_1;
    private SpaceAvDet det6_0;
    private SpaceAvDet det6_1;
    private TimeAvDet det7_0;
    private TimeAvDet det7_1;
    private SpaceAvDet det8_0;
    private SpaceAvDet det8_1;

    private StatBufDiag fcdxdv;
    private StatBufDiag Qrho;
    private StatBufDiag QrhoS;
    private StatBufDiag Qrho1;
    private StatBufDiag QrhoS1;
    private DynBufDiag fcvt;
    private DynBufDiag Qt;

    private int det2c = 0;
    private int det4c = 0;

    public SimCanvas(int choice_Szen, double density, double qIn, double perTr, double p_factor, double deltaB,
            double p_factorRamp, double deltaBRamp, int tsleep_ms) {

        newValues(choice_Szen, density, qIn, perTr, 33, p_factor, deltaB, tsleep_ms);
        this.p_factorRamp = p_factorRamp;
        this.deltaBRamp = deltaBRamp;
        tsleepFrame_ms = 10;

        lang = Language.getInstance();

        microstreet = new MicroStreet(2 * Math.PI * RADIUS_M, density, p_factor, deltaB, fcnr, choice_Szen);

    }

    public double getScale() {
        setScales();
        return (scale);
    }

    public void setScales() {

        // global sizes: xsize,ysize=dimensions of the green background
        // from Dimension getSize() method;
        // size() getSize()?
        // does not work in constructor or directly in declaration

        int xsizeOld = xsize;
        int ysizeOld = ysize;
        xsize = getSize().width;
        ysize = getSize().height;

        if (xsize < 10) {
            xsize = 10;
        } // xsize=0 at beginning -> default!!
        if (ysize < 10) {
            ysize = 10;
        } // ysize=0 at beginning -> default!!

        if (buffer == null || xsizeOld != xsize || ysizeOld != ysize) {
            buffer = createImage(xsize, ysize);
        }

        // determine physical road dimensions

        double xsize_m = 500;
        double ysize_m = 500;

        xsize_m = 2 * RADIUS_M;
        ysize_m = 2 * RADIUS_M + ((choice_Szen == 2) ? 3 : 2) * LANEWIDTH_M;
        xsize_m *= (1 + 2 * REL_ROAD_MARGIN);
        ysize_m *= (1 + 2 * REL_ROAD_MARGIN);
        scalex = xsize / xsize_m;
        scaley = ysize / ysize_m;
        scale = scalex < scaley ? scalex : scaley;

        l_straight_m = xsize / scale - RADIUS_M;
        rdlen_m = Math.PI * RADIUS_M + 2 * l_straight_m;
        total_rmplen_m = RADIUS_M * (1 + REL_ROAD_MARGIN) + L_RAMP_M;

        // determine pixel road dimensions

        rdlen = rdlen_m * scale;
        total_rmplen = (int) (total_rmplen_m * scale);
        lRamp = (int) (L_RAMP_M * scale);
        l_straight = (int) (l_straight_m * scale);
        roadMargin = (int) (REL_ROAD_MARGIN * ysize);
        radius = (int) (RADIUS_M * scale);
        lw = (int) (LANEWIDTH_M * scale); // Lane width
        r1 = radius - lw; // inner radius
        r2 = radius + lw; // outer radius
        xc = roadMargin + lw + radius; // center of circle or semicircle
        yc = roadMargin + lw + radius; // (circle and U Scenarios)

        if (false) {
            System.out.println("SimCanvas.setScales:" + " xsize_m=" + xsize_m + " xsize=" + xsize + " ysize=" + ysize
                    + ", l_straight_m=" + l_straight_m + ", rdlen_m=" + rdlen_m);
        }

        // vehicles (length varies from veh to vehicle)

        vehWidth = (int) (VEH_WIDTH_M * scale);
        pkwLen = (int) (PKW_LENGTH_M * scale);
        lkwLen = (int) (LKW_LENGTH_M * scale);

        // text ("Car", "Truck", not that of scrollbars)

        textHeight = (int) (1.0 * REL_TEXTHEIGHT * getSize().height);
        if (textHeight > MAX_TEXTHEIGHT)
            textHeight = MAX_TEXTHEIGHT;
        if (textHeight < MIN_TEXTHEIGHT)
            textHeight = MIN_TEXTHEIGHT;

        textFont = new Font("SansSerif", Font.PLAIN, textHeight);
        textFontSmall = new Font("SansSerif", Font.PLAIN, (int) (0.8 * textHeight + 0.5));
        metricsText = getFontMetrics(textFont);

        // ############################################
        // layout of the inset diagrams
        // ############################################

        boolean isRingroad = (choice_Szen == 1);

        int padding = (int) (0.02 * scale * RADIUS_M);
        diaHeight = (int) (0.5 * r2 - 0.5 * padding); // do not know why not 1*padding
        diaWidth = (isRingroad) ? xsize - xc - r2 - 2 * padding : (int) (0.5 * (xsize - xc)) - 3 * padding;
        diaWidth = Math.max(20, Math.min(3 * diaHeight, diaWidth));

        if (isRingroad) {
            dia1x = xc + r2 + padding;
            dia2x = dia1x;
            dia3x = dia1x;
            dia4x = dia1x;
            dia1y = padding;
            dia2y = dia1y + padding + diaHeight;
            dia3y = dia2y + padding + diaHeight;
            dia4y = dia3y + padding + diaHeight;
            System.out.println("diaHeight=" + diaHeight + "diaWidth=" + diaWidth);
        }

        else {
            dia1x = xc + r2 + padding;
            dia2x = dia1x;
            dia3x = dia1x;
            dia4x = dia1x;
            dia1y = diaHeight + padding;
            dia2y = dia1y + padding + diaHeight;
            dia3y = dia2y + padding + diaHeight;
            dia4y = dia3y + padding + diaHeight;

        }

    } // setScales

    public void run() {

        // long startTime = System.currentTimeMillis();

        System.out.println("SimCanvas.run()");
        time = 0.;
        setScales();

        boolean traffLightTurnedGreen = false;
        boolean showInsetdiagrams = SHOW_INSET_DIAG && (choice_Szen == 1);
        if (showInsetdiagrams) {
            setInsetDiagrams();
        }

        // The main loop: runs until current thread no longer runner;
        // if stop or new scenario -> stop() method applied:
        // sets runner=null -> run() method ends

        while (Thread.currentThread() == runner) {

            try {
                Thread.sleep(tsleep_ms);
            } catch (InterruptedException e) {
                ;
            }

            // simulate new timestep

            long timeBeforeSim_ms = System.currentTimeMillis();
            synchronized (microstreetLock) {

                if ((time > T_GREEN) && (choice_Szen == 5) && (traffLightTurnedGreen == false)) {
                    microstreet.open(); // traffic light turns green
                    traffLightTurnedGreen = true; // (only traffic light scenario)
                }
                microstreet.update(time, TIMESTEP_S, choice_Szen, density, qIn, perTr, p_factor, deltaB);
                if (choice_Szen == 2) {
                    onramp.update(time, TIMESTEP_S, qRamp, perTr, p_factorRamp, deltaBRamp);
                }
                if (showInsetdiagrams) {
                    updateInsetDiagrams();
                }
            }

            // calculate some times

            time += TIMESTEP_S;
            itime++;
            itSinceLastChange += 1;

            simTime_ms = (int) (System.currentTimeMillis() - timeBeforeSim_ms);

            // tsleepFrame_ms = tsleep_ms- simTime_ms - prepTime_ms - avgPaintTime_ms;
            // tsleepFrame_ms = tsleep_ms- simTime_ms;
            tsleepFrame_ms = (int) (0.5 * tsleep_ms + 0.5);
            // tsleepFrame_ms = tsleep_ms - simTime_ms - 2;

            if (tsleepFrame_ms < 2)
                tsleepFrame_ms = 2;

            // paint timestep
            // repaint() calls automatically this.update(g) -> paintSim(g);
            // witg Graphics object g which need NOT be changed; do not
            // change paint(), repaint method itself but only methods it
            // calls automatically -> update

            repaint();

            // totalTimeThisStep_ms should be the same or a little bit
            // smaller as tsleep
            // !! Cannot measure runtime of repaint -> 0 ms if
            // times logged before and after repaint()

            totalTimeThisStep_ms = simTime_ms + prepTime_ms + totalPaintTime_ms;

            if (false) {
                System.out.println("tsleep=" + tsleep_ms + ", tsleepFr=" + tsleepFrame_ms + ", simT=" + simTime_ms
                        + ", prepT=" + prepTime_ms + ", avgPaintT=" + avgPaintTime_ms + ", totPaintT="
                        + totalPaintTime_ms + ", totT=" + totalTimeThisStep_ms);
            }
        } // while (Thread.currentThread()== runner)
    } // public void run

    // called from start() method of main applet class MicroSim*

    public void start(int choice_Szen, double density) {
        System.out.println("SimCanvas.start(choice_Szen=" + choice_Szen + ", density=" + density + ")");
        microstreet = new MicroStreet(rdlen_m, density, p_factor, deltaB, fcnr, choice_Szen);
        if ((choice_Szen == 3) || (choice_Szen == 4) || (choice_Szen == 5)) {
            imposeSpeedLimit();
        }
        if (choice_Szen == 2) {
            double mergingpos = Math.PI * RADIUS_M + l_straight_m + 0.5 * L_RAMP_M;
            System.out.println("SimCanvas.start: mergingpos=" + mergingpos + ",l_straight_m=" + l_straight_m
                    + ",total_rmplen_m=" + total_rmplen_m);
            onramp = new OnRamp(microstreet, total_rmplen_m, L_RAMP_M, mergingpos, p_factorRamp, deltaBRamp);
        }

        if (runner == null) {
            runner = new Thread(this);
            runner.start();
        }
    }// public void start

    public void applyLocalPerturbation() {
        microstreet.applyLocalPerturbation();
    }

    // called from stop() method of main applet class MicroSim*
    public void stop() {
        System.out.println("SimCanvas.stop()");
        if (runner != null) {
            Thread waitFor = runner;
            runner = null; // kill thread (seems necessary)
            // -> start=start from new
            try {
                waitFor.join(tsleep_ms);
            } catch (InterruptedException e) {
            }
        }
    }

    // update necessary!! repaint() [standard mechanism for animation]
    // calls per default update(g),
    // and the standard update(g) per default
    // clears everything before it calls
    // paint(); oversede with own update(g) which does not clear prior
    // to painting!

    public void update(Graphics g) {
        if (!DOUBLEBUF) {
            paintSim(g);
        } else {
            paintBuffer(g);
        }
    }

    // only called if display window changed or reactivated;
    // otherwise update() called for painting

    public void paint(Graphics g) {
        // System.out.println("SimCanvas.paint()");
        itSinceLastChange = 0;
        paintBuffer(g); // also if no double buffering!
    }

    private void paintBuffer(Graphics g) {

        // make buffered graphics object bg "Doppelpufferung"

        if (buffer == null) {
            buffer = createImage(xsize, ysize);
        }

        bufferedGraphics = buffer.getGraphics();
        if (choice_Geom == 0) {
            paintCircle(bufferedGraphics);
        }
        if (choice_Geom == 1) {
            paintU(bufferedGraphics);
        }
        paintSim(bufferedGraphics); // paint into buffer
        g.drawImage(buffer, 0, 0, this); // display buffer
    }

    // paint actual road simulation to Graphics object g
    // which may be screen or buffer

    private void paintSim(Graphics g) {
        boolean showInsetdiagrams = SHOW_INSET_DIAG && (choice_Szen == 1);

        synchronized (microstreetLock) {

            if (true) {
                // if ((itSinceLastChange>=1) && (time>=1*TIMESTEP_S)) {
                long timeBeforePrep_ms = System.currentTimeMillis();
                prepareVehData();
                prepTime_ms = (int) (System.currentTimeMillis() - timeBeforePrep_ms);

                long timeBeforePaint_ms = System.currentTimeMillis();
                if (choice_Geom == 0) {
                    updateCircle(g);
                }
                if (choice_Geom == 1) {
                    updateU(g);
                }
                totalPaintTime_ms = (int) (System.currentTimeMillis() - timeBeforePaint_ms);
            }

            else {
                // !! repainting action after returning to applet;
                // paint also the vehicles; therefore also the updte.. calls
                if (choice_Geom == 0) {
                    paintCircle(g);
                    updateCircle(g);
                }
                if (choice_Geom == 1) {
                    paintU(g);
                    updateU(g);
                }
            }

            paintLegend(g);
            paintTimes(g);
            paintSymbols(g);
            if (VEH_COLORCODE == 1) {
                if (choice_Geom == 0) {
                    paintColormapSpeed(g, ((double) (xc)) / xsize + 0.05, 0.75, 0.2, 0.10);
                } else {
                    paintColormapSpeed(g, 0.5, 0.2, 0.2, 0.10);
                }
            }
            if (showInsetdiagrams) {
                paintInsetDiagrams(g);
            }
            if (((choice_Szen == 3) || (choice_Szen == 4) || (choice_Szen == 5)) && (v0_limit != v0_limit_old)) {
                imposeSpeedLimit(); // !!! Bring outside of drawing routine; not confuse
                // action with painting!!
                paintSpeedLimit(g);
            }
        }
    } // private void paintSim

    // ################################################################
    // paint* and update* paint street and all vehicles for closed systems
    // First time: call paintCircle, paintU etc: Street, grass + vehicles
    // After this: call updateCircle etc: Only cars moved
    // ################################################################

    private void paintCircle(Graphics g) {

        g.setColor(SIM_BG_COLOR); // green grass
        g.fillRect(0, 0, xsize, ysize);

        // paintCircleRoad(g);

        g.setColor(roadColor); // grey circle with outer diameter
        g.fillOval(xc - r2, yc - r2, 2 * r2, 2 * r2);
        g.setColor(SIM_BG_COLOR); // green with inner diameter
        g.fillOval(xc - r1, yc - r1, 2 * r1, 2 * r1);

        drawLines(g);
    }

    private void paintCircleRoad(Graphics g) {
        System.out.println("Simcanvas.paintCircleRoad...");
        final int NPOINTS = 50;
        int[] xpoints = new int[2 * NPOINTS + 2];
        int[] ypoints = new int[2 * NPOINTS + 2];

        for (int i = 0; i < NPOINTS + 1; i++) {
            DetPosC dp = new DetPosC(rdlen_m * i / NPOINTS, LANEWIDTH_M);
            xpoints[i] = dp.x;
            ypoints[i] = dp.y;
        }
        for (int i = NPOINTS + 1; i < 2 * NPOINTS + 2; i++) {
            DetPosC dp = new DetPosC(rdlen_m * (i - 2 * NPOINTS - 1) / NPOINTS, -LANEWIDTH_M);
            xpoints[i] = dp.x;
            ypoints[i] = dp.y;
        }

        g.setColor(roadColor);
        g.fillPolygon(xpoints, ypoints, 2 * NPOINTS + 2);
        drawLines(g);

    }

    private void updateCircle(Graphics g) {

        // calculate rectangles for new and old vehicle positions and draw.
        // Remove a little
        // bit more (make old rectangle larger); otherwise lost vehicle
        // pixels might appear; pos = back (!) edge of vehicle
        // same nr -> same length -> need no old length!
        boolean showInsetdiagrams = SHOW_INSET_DIAG && (choice_Szen == 1);

        Vector colors = microstreet.colors;
        int imax = colors.size() - 1;
        int sumPaintTime_ms = 0;

        long timeBeforePaint_ms = System.currentTimeMillis();

        double weightNew = 1;
        double weightOld = 0;

        for (int i = imax; i >= 0; i--) {
            PolyVehCircle pvc = new PolyVehCircle(pos[i], lane[i], lengthVeh[i], VEH_WIDTH_M, r1, r2, xc, yc);

            int car_xPoints[] = pvc.xPoints;
            int car_yPoints[] = pvc.yPoints;
            if ((showInsetdiagrams) && (nr[i] == fcnr)) {
                g.setColor(Color.blue);
            } else {
                g.setColor((Color) (colors.elementAt(i)));
            }
            g.fillPolygon(car_xPoints, car_yPoints, 4);

            // martin
            Color vehColor = (showInsetdiagrams && (nr[i] == fcnr)) ? Color.blue : (Color) (colors.elementAt(i));
            String vehType = (lengthVeh[i] < 8) ? "car" : "truck";
            VehicleImage2D vehimg = new VehicleImage2D(lengthVeh[i], VEH_WIDTH_M, vehType, false, vehColor, scale);
            vehimg.setPositionPix(pvc.xCenter, pvc.yCenter);
            vehimg.setDirection(pvc.phi);
            vehimg.setSpeed(speed[i]);
            if (nr[i] == fcnr) {
                vehimg.setProbeveh();
            }
            if (nr[i] == fcnr) {
                vehimg.setProbeveh();
            }
            vehimg.draw(g, VEH_COLORCODE);
        }

        paintTime_ms = (int) (System.currentTimeMillis() - timeBeforePaint_ms);
        sumPaintTime_ms += paintTime_ms;
        avgPaintTime_ms = sumPaintTime_ms;
        try {
            Thread.sleep(tsleepFrame_ms);
        } catch (InterruptedException e) {
        }
        ;

        // remove old cars

        if (itime < 2) {
            paintCircleRoad(g);
            drawLines(g);
        }

    } // updateCircle

    private void paintU(Graphics g) {

        g.setColor(SIM_BG_COLOR); // green grass everywhere
        g.fillRect(0, 0, xsize, ysize);

        g.setColor(roadColor); // paint circle as in paintCircle
        g.fillOval(xc - r2, yc - r2, 2 * r2, 2 * r2);
        g.setColor(SIM_BG_COLOR);
        g.fillOval(xc - r1, yc - r1, 2 * r1, 2 * r1);

        g.fillRect(xc, 0, xsize, ysize); // let grass grow on right side

        g.setColor(roadColor); // two straight road sections
        g.fillRect(xc, yc - r2, l_straight, 2 * lw);
        g.fillRect(xc, yc + r1, l_straight, 2 * lw);

        // draw on-ramp for Szenario 2

        if (choice_Szen == 2) {
            g.setColor(roadColor);
            g.fillRect(0, yc + r2, xc + lRamp, lw);
            int[] poly_x = { xc + lRamp, xc + lRamp, xc + lRamp + 2 * lw };
            int[] poly_y = { yc + r2, yc + r2 + lw, yc + r2 };
            g.fillPolygon(poly_x, poly_y, poly_x.length);
        }

        // lane-separating road lines

        drawLines(g);

        // speed-limit sign for Scen 3..5

        if ((choice_Szen == 3) || (choice_Szen == 4) || (choice_Szen == 5)) {
            paintSpeedLimit(g);
        }

    }

    // ################################################
    private void updateU(Graphics g) {
        // ################################################

        boolean showInsetdiagrams = SHOW_INSET_DIAG && (choice_Szen == 1);

        Vector colors = microstreet.colors;
        int imax = colors.size() - 1;
        int sumPaintTime_ms = 0;

        // paint mainroad and possibly ramp vehicles

        long timeBeforePaint_ms = System.currentTimeMillis();

        double weightNew = 1;
        double weightOld = 0;

        // draw main road vehicles

        for (int i = imax; i >= 0; i--) {
            PolyVehU pvu = new PolyVehU(pos[i], lane[i], lengthVeh[i], VEH_WIDTH_M, r1, r2, xc, l_straight, yc,
                    (int) (rdlen));

            // martin
            Color vehColor = (showInsetdiagrams && (nr[i] == fcnr)) ? Color.blue : (Color) (colors.elementAt(i));

            // ########################################
            // paint vehicle polygon at new position
            // ########################################

            String vehType = (lengthVeh[i] < 8) ? "car" : "truck";
            if (vehColor.equals(Color.white)) {
                vehType = "obstacle";
            } // hack
            VehicleImage2D vehimg = new VehicleImage2D(lengthVeh[i], VEH_WIDTH_M, vehType, false, vehColor, scale);
            vehimg.setPositionPix(pvu.xCenter, pvu.yCenter);
            vehimg.setDirection(pvu.phi);
            vehimg.setSpeed(speed[i]);
            vehimg.draw(g, 1);

        } // road vehicles

        // if choice_Szen=2, paint ramp vehicles

        if (choice_Szen == 2) {
            int yTopOn = yc + r2 + (int) (0.25 * lw);

            // paint new merging region
            // (otherwise paint of changed veh not removed)

            int xmerge = xc;
            int lmerge = lRamp;
            // g.setColor(roadColor);
            // g.fillRect(xmerge,yc+r2,lmerge,lw);

            int imaxOn = onramp.colors.size() - 1;
            for (int i = imaxOn; i >= 0; i--) {

                String vehType = (lengthVehOn[i] < 8) ? "car" : "truck";
                Color vehColor = (Color) (onramp.colors.elementAt(i));
                double speed = (Double) (onramp.velocities.elementAt(i));
                int xc_veh = xc + lRamp + (int) (scale * (0.5 * lengthVehOn[i] + posOn[i] - onramp.roadLength));
                int yc_veh = yc + r2 + (int) (0.25 * lw + scale * VEH_WIDTH_M);
                VehicleImage2D vehimg = new VehicleImage2D(lengthVehOn[i], VEH_WIDTH_M, vehType, false, vehColor, scale);
                vehimg.setPositionPix(xc_veh, yc_veh);
                vehimg.setDirection(0);
                vehimg.setSpeed(speed);
                vehimg.draw(g, 1);
            }
        }// end ramp vehicles

        // draw white lines

        if (itime < 2) {
            drawLines(g);
        }

        paintTime_ms = (int) (System.currentTimeMillis() - timeBeforePaint_ms);
        sumPaintTime_ms += paintTime_ms;
        avgPaintTime_ms = sumPaintTime_ms;
        try {
            Thread.sleep(tsleepFrame_ms);
        } catch (InterruptedException e) {
        }
        ;

    } // end updateU

    private void prepareVehData() {

        Vector positions = microstreet.positions;
        Vector velocities = microstreet.velocities;
        Vector numbers = microstreet.numbers;
        Vector lanes = microstreet.realLanes; // here (drawing) lanes always real-valued!
        Vector lengths = microstreet.lengths;

        Vector old_positions = microstreet.old_pos;
        Vector old_numbers = microstreet.old_numbers;

        int imax = positions.size() - 1;
        int imaxOn = 0;
        if (choice_Szen == 2) {
            imaxOn = onramp.positions.size() - 1;
        }

        if (imax > imaxinit) {
            System.out.println("imax>imaxinit!! Doubling array size...");
        }

        // define the arrays

        for (int i = imax; i >= 0; i--) {

            pos[i] = ((Double) positions.elementAt(i)).doubleValue();
            speed[i] = ((Double) velocities.elementAt(i)).doubleValue();
            lane[i] = ((Double) lanes.elementAt(i)).doubleValue();
            lengthVeh[i] = ((Double) lengths.elementAt(i)).doubleValue();
            nr[i] = ((Integer) numbers.elementAt(i)).intValue();

            // determine vector index of same car in the past time step

            old_i[i] = i;
            int old_nr = ((Integer) old_numbers.elementAt(i)).intValue();
            if (old_nr != nr[i]) { // overtaking during last step -> changed pos
                old_i[i] = (i + 1 <= imax) ? (i + 1) : 0; // old vehicle number at i+1?
                old_nr = ((Integer) old_numbers.elementAt(old_i[i])).intValue();

                if (old_nr != nr[i]) { // veh index not at i+1 -> must be at i-1
                    old_i[i] = (i - 1 >= 0) ? (i - 1) : imax;
                    old_nr = ((Integer) old_numbers.elementAt(old_i[i])).intValue();
                }
            }

            if (old_nr != nr[i]) { // noy yet ordered?
                System.out.println("prepareVehData, 2nd ordering round!!");
                for (int j = 2; ((j <= 5) && (old_nr != nr[i])); j++) {
                    old_i[i] = (i + j <= imax) ? (i + j) : 0;
                    old_nr = ((Integer) old_numbers.elementAt(old_i[i])).intValue();
                }
                if (old_nr != nr[i]) {
                    System.out.println("prepareVehData, 2nd ordering round,2!!");
                    for (int j = -2; ((j >= -5) && (old_nr != nr[i])); j--) {
                        old_i[i] = (i + j >= 0) ? (i + j) : imax;
                        old_nr = ((Integer) old_numbers.elementAt(old_i[i])).intValue();
                    }
                }
                if (old_nr != nr[i]) {
                    System.out.println("Error! old vehicle number not in neighbourhood!");
                }
            }

            old_pos[i] = ((Double) old_positions.elementAt(old_i[i])).doubleValue();
        }

        // on-ramp data

        if (choice_Szen == 2) {
            for (int i = imaxOn; i >= 0; i--) {
                posOn[i] = ((Double) onramp.positions.elementAt(i)).doubleValue();
                lengthVehOn[i] = ((Double) onramp.lengths.elementAt(i)).doubleValue();
            }
        }

    }// private void prepareVehData

    // draw lane-separating road lines

    private void drawLines(Graphics g) {

        g.setColor(Color.white);
        int lineLength = (int) (scale * LINELENGTH_M);
        int gapLength = (int) (scale * GAPLENGTH_M);
        int lineWidth = 1;
        int numberOfLines = (int) (rdlen / (scale * (LINELENGTH_M + GAPLENGTH_M)));

        if (choice_Geom == 0) {
            for (int i = 0; i < numberOfLines; i++) {
                double pos_m = i * (LINELENGTH_M + GAPLENGTH_M);
                PolyVehCircle roadLine = new PolyVehCircle(pos_m, -1, lineLength, lineWidth, r1, r2, xc, yc);
                g.fillPolygon(roadLine.xPoints, roadLine.yPoints, 4);
            }
        }

        if (choice_Geom == 1) {
            for (int i = 0; i < numberOfLines; i++) {
                double pos_m = i * (LINELENGTH_M + GAPLENGTH_M);
                PolyVehU roadLine = new PolyVehU(pos_m, -1, lineLength, lineWidth, r1, r2, xc, l_straight, yc,
                        (int) (rdlen));
                g.fillPolygon(roadLine.xPoints, roadLine.yPoints, 4);
            }
        }
    }

    public void newValues(int choice_Szen, double density, double qIn, double perTr, double v0_limit, double p_factor,
            double deltaB, int tsleep_ms) {

        this.choice_Szen = choice_Szen;
        this.density = density;
        this.qIn = qIn;
        this.perTr = perTr;
        this.v0_limit = v0_limit;
        this.p_factor = p_factor;
        this.deltaB = deltaB;
        this.tsleep_ms = tsleep_ms;

        choice_Geom = ((choice_Szen == 1) || (choice_Szen == 6)) ? 0 : 1;
        setScales(); // NOT after update of rdlen, but behind choice_Szen
        rdlen_m = (choice_Geom == 0) ? 2 * Math.PI * RADIUS_M : Math.PI * RADIUS_M + 2 * l_straight_m;

        System.out.println("SimCanvas.newValues():" + " choice_Szen= " + choice_Szen + " choice_Geom= " + choice_Geom);

    } // newValues

    public void newValues2(double qIn, double perTr, double p_factor, double deltaB, double qRamp, double p_factorRamp,
            double deltaBRamp, int tsleep_ms) {

        this.choice_Szen = 2; // newValues2 only called if choice_Szen==2
        setScales(); // NOT after update of rdlen, but behind choice_Szen
        this.qIn = qIn;
        this.perTr = perTr;
        this.p_factor = p_factor;
        this.qRamp = qRamp;
        this.p_factorRamp = p_factorRamp;
        this.deltaB = deltaB;
        this.deltaBRamp = deltaBRamp;
        this.tsleep_ms = tsleep_ms;
        choice_Geom = 1;
        rdlen_m = Math.PI * RADIUS_M + 2 * l_straight_m;
        System.out.println("SimCanvas.newValues2():" + " choice_Szen= " + choice_Szen + " choice_Geom= " + choice_Geom);
    } // newValues2

    // martin jan05
    // change externally IDM parameters ONLY for cars

    public void changeIDMCarparameters(IDM idm) {
        System.out.println("in SimCanvas.changeIDMCarparameters");
        ((IDM) (microstreet.idmCar)).set_params(idm);
    }

    // martin oct07: changeIDMTruckparameters noch DOS wg
    // temp_car.setModel(..) in MicroStreet.java

    public void changeIDMTruckparameters(IDM idm) {
        System.out.println("in SimCanvas.changeIDMTruckparameters");
        ((IDM) (microstreet.idmTruck)).set_params(idm);
    }

    private void imposeSpeedLimit() {
        v0_limit_old = v0_limit;

        double v0trucks = (v0_limit < VMAX_TRUCK_KMH / 3.6) ? v0_limit : VMAX_TRUCK_KMH / 3.6;

        if ((choice_Szen == 3) || (choice_Szen == 5)) {
            ((IDM) (microstreet.idmCar)).set_v0(v0_limit);
            ((IDM) (microstreet.idmCar)).initialize();
            // comment following 2 lines if trucks are "free"
            ((IDM) (microstreet.idmTruck)).set_v0(v0trucks);
            ((IDM) (microstreet.idmTruck)).initialize();
        }
        if (choice_Szen == 4) {
            ((IDM) (microstreet.sync1Car)).set_v0(v0_limit);
            ((IDM) (microstreet.sync1Car)).initialize();
            // comment following 2 lines if trucks are "free"
            ((IDM) (microstreet.sync1Truck)).set_v0(v0trucks);
            ((IDM) (microstreet.sync1Truck)).initialize();
        }

    }

    // paint "Car" and "Truck" symbols and labels

    private void paintLegend(Graphics g) {

        double xRelRight = 0.98; // right end of the area for the legend
        double yRelSpaceBot = 0.02; // space between U road and bottom of area
        if (choice_Szen == 5) {
            yRelSpaceBot = 0.10;
        }
        // boolean withProbeCar=((choice_Szen==5)||(choice_Szen==1));
        boolean withProbeCar = false;
        String carString = lang.getCarString();
        String truckString = lang.getTruckString();
        String idString = lang.getIdString();
        String probeString = lang.getProbeVehString();

        int lineHeight = (int) (1.25 * metricsText.stringWidth("M"));
        int maxLenCarTruck = Math.max(metricsText.stringWidth(truckString), metricsText.stringWidth(carString));
        int xoff = (withProbeCar) ? metricsText.stringWidth(probeString) + pkwLen : maxLenCarTruck + pkwLen;
        if (choice_Szen == 6) {
            xoff = metricsText.stringWidth(idString) + pkwLen;
        }

        int x0 = (int) (xRelRight * xsize) - xoff - lkwLen; // xsize,ysize in pixels
        int nUp = (withProbeCar) ? 2 : 1;
        int y0 = yc + r1 - (int) (yRelSpaceBot * ysize) - nUp * lineHeight;
        // System.out.println("y0="+y0);
        if (choice_Szen == 6) {
            y0 += lineHeight;
        }

        if (choice_Szen == 1) {
            x0 = (int) (xc - 0.5 * maxLenCarTruck);
            y0 = (int) (yc + 0.7 * r1);
        }

        // text "Cars" or "Trucks"

        g.setFont(textFont);
        g.setColor(Color.black);

        g.drawString(carString, x0, y0);
        g.drawString((choice_Szen == 6) ? idString : truckString, x0, y0 + lineHeight);
        if (withProbeCar) {
            g.drawString(probeString, x0, y0 + 2 * lineHeight);
        }

        // vehicle symbols

        g.setColor(MicroStreet.colorCar);
        g.fillRect(x0 + xoff, y0 - vehWidth, pkwLen, vehWidth);
        if (choice_Szen == 6) {
            g.setColor(Color.green);
            g.fillRect(x0 + xoff, y0 + lineHeight - vehWidth, pkwLen, vehWidth);
        } else {
            g.setColor(MicroStreet.colorTruck);
            g.fillRect(x0 + xoff, y0 + lineHeight - vehWidth, lkwLen, vehWidth);
        }
        if (withProbeCar) {
            g.setColor(Color.blue);
            g.fillRect(x0 + xoff, y0 + 2 * lineHeight - vehWidth, pkwLen, vehWidth);
        }
    }

    private void paintTimes(Graphics g) {

        int min, sec;
        int x0 = 10; // start position of time display (left upper corner)
        int y0 = 20;

        min = (int) (time / 60.0);
        sec = (int) (time - min * 60.0);
        String str_time = lang.getSimTime();
        String timeString = str_time + (new Integer(min)).toString() + ((sec > 9) ? ":" : ":0")
                + (new Integer(sec)).toString();
        int widthTime = metricsText.stringWidth(str_time);
        int widthDisplay = metricsText.stringWidth(timeString);

        // clear old time

        g.setColor(SIM_BG_COLOR);
        // g.setColor(Color.white);
        g.fillRect(x0 + widthTime, y0 - textHeight, widthDisplay - widthTime, textHeight);

        // display new time

        g.setFont(textFont);
        g.setColor(Color.black);
        g.drawString(timeString, x0, y0);

        // draw "Countdown <m>:<ss>" at right bottom pos

        if (choice_Szen == 5) {
            if (time < T_GREEN) {
                min = (int) ((T_GREEN - time) / 60.0);
                sec = (int) ((T_GREEN - time) - min * 60.0);
                String countdString = "Countdown " + (new Integer(min)).toString() + ((sec > 9) ? ":" : ":0")
                        + (new Integer(sec)).toString();

                int width_strCountdown = metricsText.stringWidth("Countdown");
                int width_strTotal = metricsText.stringWidth(countdString);
                int width_strTime = width_strTotal - width_strCountdown;
                double xRelRight = 0.98;
                int xLeft = (int) (xRelRight * xsize) - width_strTotal;
                int yTop = (int) (yc + radius - lw - 1.5 * textHeight);

                g.setColor(Color.white);
                g.fillRect(xLeft + width_strCountdown, yTop, width_strTime, textHeight);
                g.setColor(Color.black);
                g.drawString(countdString, xLeft, (int) (yTop + 0.9 * textHeight));
            }
        }
    }

    private void paintSymbols(Graphics g) {

        // traffic light
        // t<T_GREEN: red; otherwise green

        if (choice_Szen == 5) {
            int heightTL = (int) (radius * 0.16); // total height of T.L.
            int radiusTL = (int) (radius * 0.024); // radius of lights
            double det1pos = RELPOS_TL * rdlen_m;
            int detx = (new DetPosU(det1pos)).x;
            int dety = (new DetPosU(det1pos)).y - lw;
            g.fillRect(detx, dety - heightTL, 2, heightTL); // pole
            g.fillRect(detx - radiusTL, dety - heightTL, 2 * radiusTL, 4 * radiusTL);

            if (time < T_GREEN) {
                g.setColor(Color.red);
                g.fillOval(detx - radiusTL, dety - heightTL, 2 * radiusTL, 2 * radiusTL);
            } else {
                g.setColor(Color.green);
                g.fillOval(detx - radiusTL, dety - heightTL + 2 * radiusTL, 2 * radiusTL, 2 * radiusTL);
            }
        }

        // paint the "virtual traffic light" controlling too much inflow

        if ((choice_Szen == 2) || (choice_Szen == 3) || (choice_Szen == 4) || (choice_Szen == 5)) {
            int bw = (int) (radius * 0.1);
            int xPix = (int) (l_straight + xc - 2 * bw);
            int yPix = (int) (yc - r1 + 5);
            if (microstreet.red == true) {
                g.setColor(Color.black);
                g.fillRect(xPix, yPix, bw, bw);
                g.setColor(Color.red);
                g.fillOval(xPix, yPix, bw, bw);
            } else {
                g.setColor(Color.black);
                g.fillRect(xPix, yPix, bw, bw);
                g.setColor(Color.green);
                g.fillOval(xPix, yPix, bw, bw);
            }
        }

        // paint Uphill traffic Sign (US)
        // for the flow-conserving bottleneck Scen.

        if (choice_Szen == 4) {
            double detpos = l_straight_m + 0.5 * Math.PI * RADIUS_M;

            // position of pole
            int heightTot = (int) (radius * 0.24); // total height of U.S.
            int heightSign = (int) (radius * 0.16); //
            int x0 = (new DetPosU(detpos)).x + (int) (0.2 * radius);
            int y0 = (new DetPosU(detpos)).y + heightTot;
            int xc = x0;
            int yc = y0 - heightTot + (int) (0.5 * heightSign);
            double relWidthRed = 0.2; //

            // draw pole
            g.setColor(Color.black);
            g.fillRect(x0, y0 - heightTot, 2, heightTot);

            // draw triangular sign
            int hbasis = (int) (0.577 * heightSign); // tan(30 deg)
            int vbasis = (int) (0.5 * heightSign);
            int[] ptsx = { xc, xc - hbasis, xc + hbasis };
            int[] ptsy = { yc - vbasis, yc + vbasis, yc + vbasis };
            g.setColor(Color.red);
            g.fillPolygon(ptsx, ptsy, ptsx.length);

            yc += (int) (0.3 * relWidthRed * vbasis);
            hbasis = (int) ((1 - relWidthRed) * hbasis);
            vbasis = (int) ((1 - relWidthRed) * vbasis);
            int[] ptsx1 = { xc, xc - hbasis, xc + hbasis };
            int[] ptsy1 = { yc - vbasis, yc + vbasis, yc + vbasis };
            g.setColor(Color.white);
            g.fillPolygon(ptsx1, ptsy1, ptsx1.length);

            // draw gradient
            ptsx1[0] = (int) (0.5 * (ptsx[0] + ptsx[2]));
            ptsy1[0] = (int) (0.5 * (ptsy[0] + ptsy[2]));

            g.setColor(Color.black);
            g.fillPolygon(ptsx1, ptsy1, ptsx1.length);

            // text

            y0 += heightSign;
            g.drawString(lang.getUphillMarkerBegin(), x0, y0);
            y0 += (int) (0.07 * radius);
            g.drawString(lang.getUmhillMarker(), x0, y0);

        }

        // paint Optional text "Engstelle" and crop marks

        if (false) {
            int x0 = (int) (0.25 * xsize);
            int y0 = (int) (0.82 * ysize);
            if (choice_Szen == 3) { // choice_Szen==3: lane closing
                g.setFont(textFont);
                g.setColor(Color.black);
                g.drawString("Engstelle", x0, y0);
            }

            int x1mark = (int) (0.10 * xsize);
            int y1mark = (int) (0.50 * ysize);
            int x2mark = (int) (0.70 * xsize);
            int y2mark = (int) (0.90 * ysize);
            int markLen = (int) (0.05 * xsize);

            if (choice_Szen == 3) { // choice_Szen==3: lane closing
                // g.setFont(textFont);
                g.setColor(Color.black);
                g.drawLine(x1mark, y1mark, x1mark + markLen, y1mark);
                g.drawLine(x2mark, y2mark, x2mark, y2mark - markLen);
            }
        }
    } // private void paintSymbols

    // paint the speed-limit sign

    private void paintSpeedLimit(Graphics g) {
        int v0_kmh = (int) (3.6 * v0_limit);
        int heightSS = (int) (radius * 0.24); // total height of T.L.
        int radiusSS = (int) (radius * 0.08); // radius of lights
        double det1pos = RELPOS_SPEEDSIGN * rdlen_m;
        int x0 = (new DetPosU(det1pos)).x; // basis of pole
        int y0 = (new DetPosU(det1pos)).y + (int) (1.5 * lw) + heightSS;
        int xc = x0; // center of circular sign
        int yc = y0 - heightSS + radiusSS; // center of circular sign

        g.setColor(Color.black);
        g.fillRect(x0, y0 - heightSS, 2, heightSS); // pole

        if (v0_kmh >= V0_LIMIT_MAX_KMH) { // no speed limit
            g.setColor(Color.white);
            g.fillOval(xc - radiusSS, yc - radiusSS, 2 * radiusSS, 2 * radiusSS);
            g.setColor(Color.black);
            int space = (int) (0.15 * radiusSS);
            for (int i = -2; i <= 2; i++) {
                g.drawLine(xc - (int) (0.7 * radiusSS) + i * space, yc + (int) (0.7 * radiusSS) + i * space, xc
                        + (int) (0.7 * radiusSS) + i * space, yc - (int) (0.7 * radiusSS) + i * space);
            }
        }

        else { // speed limit active
            int rwhite = (int) (0.8 * radiusSS);
            String speedLimitStr = String.valueOf(v0_kmh);
            int ref_width = metricsText.stringWidth(speedLimitStr);
            int speedFontHeight = (int) (textHeight * 1.4 * radiusSS / ref_width);
            Font speedFont = new Font("SansSerif", Font.BOLD, speedFontHeight);
            int xoff = (int) (0.65 * radiusSS);
            int yoff = (int) (0.4 * speedFontHeight);

            g.setColor(Color.red);
            g.fillOval(xc - radiusSS, yc - radiusSS, 2 * radiusSS, 2 * radiusSS);
            g.setColor(Color.white);
            g.fillOval(xc - rwhite, yc - rwhite, 2 * rwhite, 2 * rwhite);
            g.setColor(Color.black);
            g.setFont(speedFont);
            g.drawString(String.valueOf(v0_kmh), xc - xoff, yc + yoff);
            g.setFont(textFont);
        }
    } // private void paintSpeedlimit...

    // lane=0: left/inner
    // lane=1: right/outer
    // lane=-1: polygon for road lines
    // lane double-valued for continuous transition!

    class PolyVehCircle {

        public int xPoints[] = new int[5];
        public int yPoints[] = new int[5];

        // following public vars temporarily for VehicleImage2D (martin oct11)
        public int xCenter;
        public int yCenter;
        public double phi;
        double xc_car, yc_car;

        public PolyVehCircle(double pos, double lane, double length, double width, int r1, int r2, int xc, int yc) {

            double l = length * scale; // length of drawn car
            double w = width * scale; // width of drawn car

            double center_pos = pos + length / 2; // in sim pos=back(!) end
            double arc = -center_pos / RADIUS_M - 0.25 * (Math.PI);
            phi = (CLOCKWISE) ? -arc + 0.5 * (Math.PI) : arc - 0.5 * (Math.PI); // martin

            double ca = Math.cos(arc);
            double sa = ((CLOCKWISE) ? -1 : 1) * Math.sin(arc);
            double r = (lane >= 0) ? r1 + (0.25 + 0.5 * lane) * (r2 - r1) : r1 + 0.5 * (r2 - r1);

            xc_car = (xc + r * ca);
            yc_car = (yc + r * sa); // graph y = - math y

            int x_frontleft = (int) (xc_car + 0.5 * (-w * ca - l * sa));
            int x_frontright = (int) (xc_car + 0.5 * (w * ca - l * sa));
            int x_backleft = (int) (xc_car + 0.5 * (-w * ca + l * sa));
            int x_backright = (int) (xc_car + 0.5 * (w * ca + l * sa));

            int y_frontleft = (int) (yc_car + 0.5 * (-w * sa + l * ca));
            int y_frontright = (int) (yc_car + 0.5 * (w * sa + l * ca));
            int y_backleft = (int) (yc_car + 0.5 * (-w * sa - l * ca));
            int y_backright = (int) (yc_car + 0.5 * (w * sa - l * ca));

            xPoints[0] = x_frontright;
            xPoints[1] = x_frontleft;
            xPoints[2] = x_backleft;
            xPoints[3] = x_backright;

            yPoints[0] = y_frontright;
            yPoints[1] = y_frontleft;
            yPoints[2] = y_backleft;
            yPoints[3] = y_backright;

            // martin oct11
            xCenter = (int) (xc_car);
            yCenter = (int) (yc_car); // phi updated underway

        }
    }

    // lane=0: left/inner
    // lane=1: right/outer
    // lane=-1: polygon for road lines
    // lane double-valued for continuous transition!

    class PolyVehU {

        public int xPoints[] = new int[5];
        public int yPoints[] = new int[5];

        // following public vars temporarily for VehicleImage2D (martin oct11)
        public int xCenter;
        public int yCenter;
        public double phi;
        double xc_car, yc_car;

        public PolyVehU(double pos, double lane, double length, double width, int r1, int r2, int xc, int l_straight,
                int yc, int rdlen) {

            double l = length * scale; // length of drawn car
            double w = width * scale; // width of drawn car
            double center_pos = pos + length / 2; // in sim pos=back (!) end
            int intpos = (int) (center_pos * scale);
            double r = (lane >= 0) ? r1 + (0.25 + 0.5 * lane) * (r2 - r1) : r1 + 0.5 * (r2 - r1);
            phi = -Math.PI; // martin

            if ((intpos > l_straight) && (intpos < (rdlen - l_straight))) {

                double arc = -0.5 * (Math.PI) - (2 * (Math.PI) * ((double) (intpos - l_straight)))
                        / ((double) (2 * (rdlen - 2 * l_straight)));

                phi = arc - 0.5 * (Math.PI); // martin
                double ca = Math.cos(arc);
                double sa = Math.sin(arc);
                xc_car = (xc + r * ca);
                yc_car = (yc + r * sa);

                int x_frontleft = (int) (xc_car + 0.5 * (-w * ca - l * sa));
                int x_frontright = (int) (xc_car + 0.5 * (w * ca - l * sa));
                int x_backleft = (int) (xc_car + 0.5 * (-w * ca + l * sa));
                int x_backright = (int) (xc_car + 0.5 * (w * ca + l * sa));

                int y_frontleft = (int) (yc_car + 0.5 * (-w * sa + l * ca));
                int y_frontright = (int) (yc_car + 0.5 * (w * sa + l * ca));
                int y_backleft = (int) (yc_car + 0.5 * (-w * sa - l * ca));
                int y_backright = (int) (yc_car + 0.5 * (w * sa - l * ca));

                xPoints[0] = x_frontright;
                xPoints[1] = x_frontleft;
                xPoints[2] = x_backleft;
                xPoints[3] = x_backright;
                xPoints[4] = x_frontright;

                yPoints[0] = y_frontright;
                yPoints[1] = y_frontleft;
                yPoints[2] = y_backleft;
                yPoints[3] = y_backright;
                yPoints[4] = y_frontright;
            }
            if (intpos <= l_straight) {

                phi = -Math.PI; // martin
                double ca = 0.0;
                double sa = 1.0;

                xc_car = xc + l_straight - intpos;
                yc_car = yc - r;

                int x_frontleft = (int) (xc_car + 0.5 * (-w * ca - l * sa));
                int x_frontright = (int) (xc_car + 0.5 * (w * ca - l * sa));
                int x_backleft = (int) (xc_car + 0.5 * (-w * ca + l * sa));
                int x_backright = (int) (xc_car + 0.5 * (w * ca + l * sa));

                int y_frontleft = (int) (yc_car + 0.5 * (-w * sa + l * ca));
                int y_frontright = (int) (yc_car + 0.5 * (w * sa + l * ca));
                int y_backleft = (int) (yc_car + 0.5 * (-w * sa - l * ca));
                int y_backright = (int) (yc_car + 0.5 * (w * sa - l * ca));

                xPoints[0] = x_frontright;
                xPoints[1] = x_frontleft;
                xPoints[2] = x_backleft;
                xPoints[3] = x_backright;
                xPoints[4] = x_frontright;

                yPoints[0] = y_frontright;
                yPoints[1] = y_frontleft;
                yPoints[2] = y_backleft;
                yPoints[3] = y_backright;
                yPoints[4] = y_frontright;
            }

            if (intpos > (rdlen - l_straight)) {

                phi = 0; // martin
                double ca = 0.0;
                double sa = -1.0;

                xc_car = xc + l_straight + intpos - rdlen;
                yc_car = yc + r;

                int x_frontleft = (int) (xc_car + 0.5 * (-w * ca - l * sa));
                int x_frontright = (int) (xc_car + 0.5 * (w * ca - l * sa));
                int x_backleft = (int) (xc_car + 0.5 * (-w * ca + l * sa));
                int x_backright = (int) (xc_car + 0.5 * (w * ca + l * sa));

                int y_frontleft = (int) (yc_car + 0.5 * (-w * sa + l * ca));
                int y_frontright = (int) (yc_car + 0.5 * (w * sa + l * ca));
                int y_backleft = (int) (yc_car + 0.5 * (-w * sa - l * ca));
                int y_backright = (int) (yc_car + 0.5 * (w * sa - l * ca));

                xPoints[0] = x_frontright;
                xPoints[1] = x_frontleft;
                xPoints[2] = x_backleft;
                xPoints[3] = x_backright;
                xPoints[4] = x_frontright;

                yPoints[0] = y_frontright;
                yPoints[1] = y_frontleft;
                yPoints[2] = y_backleft;
                yPoints[3] = y_backright;
                yPoints[4] = y_frontright;
            }
            // martin oct11
            xCenter = (int) (xc_car);
            yCenter = (int) (yc_car); // phi updated underway
        }
    }

    class DetPosU { // positioning of road signs etc as function of position
        public int x;
        public int y;

        public DetPosU(double pos) {
            int intpos = (int) (pos * scale);

            if ((intpos > l_straight) && (intpos < (rdlen - l_straight))) {

                double arc = -0.5 * (Math.PI) - (2 * (Math.PI) * ((double) (intpos - l_straight)))
                        / ((double) (2 * (rdlen - 2 * l_straight)));

                double ca = Math.cos(arc);
                double sa = Math.sin(arc);

                x = (int) (xc + radius * ca);
                y = (int) (yc + radius * sa);
            }
            if (intpos <= l_straight) {
                x = (int) xc + l_straight - intpos;
                y = (int) yc - radius;
            }
            if (intpos > (rdlen - l_straight)) {

                x = (int) (xc + l_straight + intpos - rdlen);
                y = (int) (yc + radius);
            }
        }
    }

    class DetPosS {
        // positioning of road signs etc as function of position
        // Straight roads
        public int x;
        public int y;

        public DetPosS(double pos_m, int yc) {
            x = (int) (int) (pos_m * scale);
            y = yc;
        }
    } // end DetPosS

    class DetPosC {
        public int x;
        public int y;

        public DetPosC(double pos_m) {

            double arc = -pos_m / RADIUS_M - 0.25 * (Math.PI);
            double ca = Math.cos(arc);
            double sa = Math.sin(arc);

            x = (int) (xc + radius * ca); // radius in pixels
            y = (int) (yc + radius * sa);
        }

        public DetPosC(double pos_m, double rightOffset_m) {
            double rightOffset = scale * rightOffset_m;
            double arc = -pos_m / RADIUS_M - 0.25 * (Math.PI);
            double ca = Math.cos(arc);
            double sa = Math.sin(arc);

            x = (int) (xc + (radius + rightOffset) * ca);
            y = (int) (yc + (radius + rightOffset) * sa);
        }
    }

    // ########################################################
    // ########################################################
    // ########################################################

    private void setInsetDiagrams() {
        final double CIRCUMF_M = 2. * Math.PI * RADIUS_M;

        if (choice_Szen == 1) {
            det1_0 = new TimeAvDet(0.125 * CIRCUMF_M, 30.0, 0);
            det1_1 = new TimeAvDet(0.125 * CIRCUMF_M, 30.0, 1);
            det2_0 = new SpaceAvDet(0.125 * CIRCUMF_M, 50.0, 0);
            det2_1 = new SpaceAvDet(0.125 * CIRCUMF_M, 50.0, 1);
            det3_0 = new TimeAvDet(0.375 * CIRCUMF_M, 30.0, 0);
            det3_1 = new TimeAvDet(0.375 * CIRCUMF_M, 30.0, 1);
            det4_0 = new SpaceAvDet(0.375 * CIRCUMF_M, 50.0, 0);
            det4_1 = new SpaceAvDet(0.375 * CIRCUMF_M, 50.0, 1);
            det5_0 = new TimeAvDet(0.625 * CIRCUMF_M, 30.0, 0);
            det5_1 = new TimeAvDet(0.625 * CIRCUMF_M, 30.0, 1);
            det6_0 = new SpaceAvDet(0.625 * CIRCUMF_M, 50.0, 0);
            det6_1 = new SpaceAvDet(0.625 * CIRCUMF_M, 50.0, 1);
            det7_0 = new TimeAvDet(0.875 * CIRCUMF_M, 30.0, 0);
            det7_1 = new TimeAvDet(0.875 * CIRCUMF_M, 30.0, 1);
            det8_0 = new SpaceAvDet(0.875 * CIRCUMF_M, 50.0, 0);
            det8_1 = new SpaceAvDet(0.875 * CIRCUMF_M, 50.0, 1);

            diagram1 = createImage(diaWidth, diaHeight);
            diagram2 = createImage(diaWidth, diaHeight);
            diagram3 = createImage(diaWidth, diaHeight);
            diagram4 = createImage(diaWidth, diaHeight);

            fcdxdv = new StatBufDiag(-10.0, 10.0, 0.0, 100.0, diaWidth, diaHeight, 400, Color.white, Color.black,
                    diagram1, "Velocity Difference", "Headway", "", "Blue Car", 5.0, "5 m/s", 50.0, "50 m");
            String[] ylabels = { "acc", "V" };
            Color[] colors = { Color.black, Color.red };
            double[] ytics = { 4.0, 2.86 };
            String[] yticLabels = { "4 m/s^2", "20 m/s" };
            fcvt = new DynBufDiag(0.0, 60.0, -5.0, 5.0, diaWidth, diaHeight, 60.0, Color.white, Color.black, diagram2,
                    "time", ylabels, colors, "", "Acceleration+Velocity", 30.0, "-30s", ytics, yticLabels);
            Qrho = new StatBufDiag(0.0, 0.15, 0.0, 0.8, diaWidth, diaHeight, 2000, Color.white, Color.black, diagram3,
                    "rho", "Q", "Detectors D1-D4", "Data from", 0.075, "75 Veh/km", 0.417, "1500 Veh/h");
            QrhoS = new StatBufDiag(0.0, 0.15, 0.0, 0.8, diaWidth, diaHeight, 1000, Color.white, Color.black, diagram4,
                    "rho", "Q", "Detectors D1-D4", "Actual Data", 0.075, "75 Veh/km", 0.417, "1500 Veh/h");

        }
        if (choice_Szen == 5) {

            det1_0 = new TimeAvDet(0.8 * CIRCUMF_M, 30.0, 0);
            det1_1 = new TimeAvDet(0.8 * CIRCUMF_M, 30.0, 1);

            diagram1 = createImage(diaWidth, diaHeight);
            diagram2 = createImage(diaWidth, diaHeight);

            String[] ylabels = { "acc", "V" };
            Color[] colors = { Color.black, Color.red };
            double[] ytics = { 4.0, 2.86 };
            String[] yticLabels = { "4 m/s^2", "20 m/s" };
            fcvt = new DynBufDiag(0.0, 120.0, -5.0, 5.0, diaWidth, diaHeight, 120.0, Color.white, Color.black,
                    diagram1, "time", ylabels, colors, "Blue Car", "Acceleration+Velocity", 60.0, "-60s", ytics,
                    yticLabels);
            String[] ylabels1 = { "Q" };
            Color[] colors1 = { Color.black };
            double[] ytics1 = { 0.417 };
            String[] yticLabels1 = { "1500 Veh/h" };

            Qt = new DynBufDiag(0.0, 60.0, 0.0, 0.8, diaWidth, diaHeight, 60.0, Color.white, Color.black, diagram2,
                    "time", ylabels1, colors1, "", "Total Flow at D1", 30.0, "-30s", ytics1, yticLabels1);
        }

        if ((choice_Szen == 2) || (choice_Szen == 3) || (choice_Szen == 4)) {

            det1_0 = new TimeAvDet(0.9 * CIRCUMF_M, 30.0, 0);
            det1_1 = new TimeAvDet(0.9 * CIRCUMF_M, 30.0, 1);
            det2_0 = new SpaceAvDet(0.9 * CIRCUMF_M, 50.0, 0);
            det2_1 = new SpaceAvDet(0.9 * CIRCUMF_M, 50.0, 1);
            if (choice_Szen == 3) {
                det3_0 = new TimeAvDet(0.6 * CIRCUMF_M, 30.0, 0);
                det3_1 = new TimeAvDet(0.6 * CIRCUMF_M, 30.0, 1);
                det4_0 = new SpaceAvDet(0.6 * CIRCUMF_M, 50.0, 0);
                det4_1 = new SpaceAvDet(0.6 * CIRCUMF_M, 50.0, 1);
            } else {
                det3_0 = new TimeAvDet(0.4 * CIRCUMF_M, 30.0, 0);
                det3_1 = new TimeAvDet(0.4 * CIRCUMF_M, 30.0, 1);
                det4_0 = new SpaceAvDet(0.4 * CIRCUMF_M, 50.0, 0);
                det4_1 = new SpaceAvDet(0.4 * CIRCUMF_M, 50.0, 1);
            }
            diagram1 = createImage(diaWidth, diaHeight);
            diagram2 = createImage(diaWidth, diaHeight);
            diagram3 = createImage(diaWidth, diaHeight);
            diagram4 = createImage(diaWidth, diaHeight);

            // martin nov07: funddia echt gezeigt? sonst Qt(t)
            // aktiviere mit g.drawImage
            Qrho = new StatBufDiag(0.0, 0.15, 0.0, 0.8, diaWidth, diaHeight, 500, Color.white, Color.black, diagram1,
                    "rho", "Q", "Detector at D2", "Measured Data", 0.075, "75 Veh/km", 0.417, "1500 Veh/h");

            if (false) {
                // martin nov07: aktiviere, falls Q(t) getzeigt
                // aktiviere mit g.drawImage

                String[] ylabels1 = { "Q" };
                Color[] colors1 = { Color.black };
                double[] ytics1 = { 0.417 };
                String[] yticLabels1 = { "1500 Veh/h" };

                Qt = new DynBufDiag(0.0, 0.15, 0.0, 0.8, diaWidth, diaHeight, 60.0, Color.white, Color.black, diagram1,
                        "time", ylabels1, colors1, "", "Total Flow at D1", 30.0, "-30s", ytics1, yticLabels1);
            }

            QrhoS = new StatBufDiag(0.0, 0.15, 0.0, 0.8, diaWidth, diaHeight, 100, Color.white, Color.black, diagram2,
                    "rho", "Q", "Detector at D2", "Actual Data", 0.075, "75 Veh/km", 0.417, "1500 Veh/h");
            Qrho1 = new StatBufDiag(0.0, 0.15, 0.0, 0.8, diaWidth, diaHeight, 500, Color.white, Color.black, diagram3,
                    "rho", "Q", "Detector at D1", "Measured Data", 0.075, "75 Veh/km", 0.417, "1500 Veh/h");
            QrhoS1 = new StatBufDiag(0.0, 0.15, 0.0, 0.8, diaWidth, diaHeight, 100, Color.white, Color.black, diagram4,
                    "rho", "Q", "Detector at D1", "Actual Data", 0.075, "75 Veh/km", 0.417, "1500 Veh/h");

        }
    }

    private void updateInsetDiagrams() {
        if (choice_Szen == 1) {
            det1_0.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det1_1.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det2_0.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);
            det2_1.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);
            det3_0.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det3_1.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det4_0.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);
            det4_1.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);
            det5_0.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det5_1.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det6_0.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);
            det6_1.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);
            det7_0.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det7_1.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det8_0.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);
            det8_1.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);

            double fcdx = (microstreet.fcd) - 5.0;
            double fcvd = -(microstreet.fcvd);
            double fcacc = microstreet.fcacc;
            double fcv = microstreet.fcvel;
            fcvt.addPoint(time, fcacc, 0);
            fcvt.addPoint(time, (fcv / 7.0), 1);
            fcdxdv.addPoint(fcvd, fcdx);

            double Q = det1_0.flow();
            double V = det1_0.harmVel();
            double rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }
            Q = det1_1.flow();
            V = det1_1.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }
            Q = det3_0.flow();
            V = det3_0.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }
            Q = det3_1.flow();
            V = det3_1.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }
            Q = det5_0.flow();
            V = det5_0.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }
            Q = det5_1.flow();
            V = det5_1.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }
            Q = det7_0.flow();
            V = det7_0.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }
            Q = det7_1.flow();
            V = det7_1.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }

            rho = det2_0.density();
            V = det2_0.avVel();
            QrhoS.addPoint(rho, rho * V);
            rho = det2_1.density();
            V = det2_1.avVel();
            QrhoS.addPoint(rho, rho * V);

            rho = det4_0.density();
            V = det4_0.avVel();
            QrhoS.addPoint(rho, rho * V);
            rho = det4_1.density();
            V = det4_1.avVel();
            QrhoS.addPoint(rho, rho * V);

            rho = det6_0.density();
            V = det6_0.avVel();
            QrhoS.addPoint(rho, rho * V);
            rho = det6_1.density();
            V = det6_1.avVel();
            QrhoS.addPoint(rho, rho * V);

            rho = det8_0.density();
            V = det8_0.avVel();
            QrhoS.addPoint(rho, rho * V);
            rho = det8_1.density();
            V = det8_1.avVel();
            QrhoS.addPoint(rho, rho * V);
        }

        if (choice_Szen == 5) {

            det1_0.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det1_1.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);

            double fcdx = (microstreet.fcd) - 5.0;
            double fcvd = -(microstreet.fcvd);
            double fcacc = microstreet.fcacc;
            double fcv = microstreet.fcvel;

            double Q = (det1_0.flow());
            Q = Q + (det1_1.flow());

            fcvt.addPoint(time, fcacc, 0);
            fcvt.addPoint(time, (fcv / 7.0), 1);
            // fcdxdv.addPoint(fcvd, fcdx);
            Qt.addPoint(time, Q, 0);
        }

        if ((choice_Szen == 2) || (choice_Szen == 3) || (choice_Szen == 4)) {

            det1_0.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det1_1.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det2_0.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);
            det2_1.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);

            det3_0.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det3_1.update(microstreet.positions, microstreet.old_pos, microstreet.velocities, microstreet.lanes, time);
            det4_0.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);
            det4_1.update(microstreet.positions, microstreet.distances, microstreet.velocities, microstreet.lanes);

            double Q = det1_0.flow();
            double V = det1_0.harmVel();
            double rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }
            Q = det1_1.flow();
            V = det1_1.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho.addPoint(rho, Q);
            }

            Q = det3_0.flow();
            V = det3_0.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho1.addPoint(rho, Q);
            }
            Q = det3_1.flow();
            V = det3_1.harmVel();
            rho = 0;
            if (V > 0.0) {
                rho = Q / V;
                Qrho1.addPoint(rho, Q);
            }

            rho = det2_0.density();
            V = det2_0.avVel();
            QrhoS.addPoint(rho, rho * V);
            rho = det2_1.density();
            V = det2_1.avVel();
            QrhoS.addPoint(rho, rho * V);

            rho = det4_0.density();
            V = det4_0.avVel();
            QrhoS1.addPoint(rho, rho * V);
            rho = det4_1.density();
            V = det4_1.avVel();
            QrhoS1.addPoint(rho, rho * V);

        }
    }

    private void paintInsetDiagrams(Graphics g) {
        final double CIRCUMF_M = 2. * Math.PI * RADIUS_M;

        if (choice_Szen == 1) {

            // actual diagrams

            g.drawImage(fcdxdv.picture(), dia1x, dia1y, null);
            g.drawImage(fcvt.picture(), dia2x, dia2y, null);
            g.drawImage(Qrho.picture(), dia3x, dia3y, null);
            g.drawImage(QrhoS.picture(), dia4x, dia4y, null);

            // labels "D1" (top) ... "D4" (right)

            int detx, dety;
            int distance = (int) (15.0 * scale); // distance from road (15 m)
            int xoff = metricsText.stringWidth("D2");
            int yoff = (int) (0.8 * metricsText.stringWidth("M"));
            g.setFont(textFont);
            g.setColor(Color.black);

            detx = (new DetPosC(0.125 * CIRCUMF_M)).x;
            dety = (new DetPosC(0.125 * CIRCUMF_M)).y;
            g.drawString("D1", detx - (int) (0.5 * xoff), dety + distance + yoff);

            detx = (new DetPosC(0.375 * CIRCUMF_M)).x;
            dety = (new DetPosC(0.375 * CIRCUMF_M)).y;
            g.drawString("D2", detx + distance, dety + (int) (0.5 * yoff));

            detx = (new DetPosC(0.625 * CIRCUMF_M)).x;
            dety = (new DetPosC(0.625 * CIRCUMF_M)).y;
            g.drawString("D3", detx - (int) (0.5 * xoff), dety - distance);

            detx = (new DetPosC(0.875 * CIRCUMF_M)).x;
            dety = (new DetPosC(0.875 * CIRCUMF_M)).y;
            g.drawString("D4", detx - distance - xoff, dety + (int) (0.5 * yoff));

        }

        // traffic light scenario; 2 diagrams

        if (choice_Szen == 5) {
            g.drawImage(fcvt.picture(), dia1x, dia1y, null);
            g.drawImage(Qt.picture(), dia2x, dia2y, null);

            // label "D1" at the position of the detector

            double det1pos = 0.8 * CIRCUMF_M;
            int detx = (new DetPosU(det1pos)).x;
            int dety = (new DetPosU(det1pos)).y;
            g.setFont(textFont);
            g.setColor(Color.black);
            g.drawString("D1", detx, dety);

        }

        if ((choice_Szen == 2) || (choice_Szen == 3) || (choice_Szen == 4)) {

            g.drawImage(Qrho1.picture(), dia1x, dia1y, null);
            g.drawImage(QrhoS1.picture(), dia2x, dia2y, null);
            g.drawImage(Qrho.picture(), dia3x, dia3y, null);
            g.drawImage(Qrho.picture(), dia4x, dia4y, null);
            // g.drawImage (Qt.picture(), dia4x,dia4y,null); //martin nov07

            double detpos = (choice_Szen == 4) ? 0.4 * CIRCUMF_M : 0.6 * CIRCUMF_M;
            int detx = (new DetPosU(detpos)).x;
            int dety = (new DetPosU(detpos)).y;
            g.setFont(textFont);
            g.setColor(Color.black);
            g.drawString("D1", detx, dety);
            detpos = 0.9 * CIRCUMF_M;
            detx = (new DetPosU(detpos)).x;
            dety = (new DetPosU(detpos)).y;
            g.setFont(textFont);
            g.setColor(Color.black);
            g.drawString("D2", detx, dety);
        }

    }

    // end paintInsetDiagrams

    /**
     * draw the speed color code if colorCode==1
     * if condition is at the calling location,
     * calls final method VehicleImage2D.colormapSpeed(...)
     * xrel=0..1 from left, yrel=0..1 from bottom
     */

    private void paintColormapSpeed(Graphics g, double xrelCenter, double yrelCenter, double widthRel, double heightRel) {

        int x0 = (int) (xrelCenter * xsize);
        int y0 = (int) ((1 - yrelCenter) * ysize);
        double w = widthRel * xsize; // double; rounded at the end
        double h = heightRel * ysize;
        int xLeft = (int) (x0 - 0.5 * w);
        int xText = (int) (x0 + 0.05 * w);
        int yTop = (int) (y0 - 0.5 * h);
        int wBox = (int) (0.5 * w); // hBox=below

        String speedString = lang.getSpeed();

        double vmin = VehicleImage2D.SPEEDMIN_MS;
        double vmax = VehicleImage2D.SPEEDMAX_MS;

        // draw the colorcode box

        final int NSAMPLE = 20;
        for (int i = 0; i < NSAMPLE; i++) {

            // following two line avoid accumulation of rounding errors
            int hBox = (int) (h * (i + 1) / NSAMPLE) - (int) (h * i / NSAMPLE);
            int yTopBox = yTop + (int) (h * i / NSAMPLE);

            double v = (NSAMPLE - (i + 0.5)) * vmax / NSAMPLE;
            Color speedColor = VehicleImage2D.colormapSpeed(v, vmin, vmax);
            g.setColor(speedColor);
            g.fillRect(xLeft, yTopBox, wBox, hBox);
        }

        // draw the speed value strings

        final int NSTRING = 3;
        g.setFont(textFontSmall);
        g.setColor(Color.black);
        for (int i = 0; i < NSTRING; i++) {
            int v_kmh = (int) (3.6 * (NSTRING - 1 - i) * vmax / (NSTRING - 1) + 0.5);
            String vkmh_str = (new Integer(v_kmh)).toString();
            String vText = vkmh_str + " km/h";
            int x_str = xText;
            int y_str = yTop + (int) (0.95 * h * (i + 0.2) / (NSTRING - 1));
            g.drawString(vText, x_str, y_str);
        }

    } // paintColormapSpeed
}
