/*
 * Copyright (C) 2006-2012 by Martin Treiber, <treiber@vwi.tu-dresden.de>
 * ---------------------------------------------------------------------------------
 * 
 * This file is part of the simulation project
 * 
 * www.traffic-simulation.de
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MovSim. If not, see <http://www.gnu.org/licenses/>
 * or <http://www.traffic-simulation.de>.
 * 
 * ---------------------------------------------------------------------------------
 */
package de.trafficsimulation.core;

import java.util.Vector;

public class IntpDet {

    private double pos_up, dist_up, V_up, pos_down, dist_down, V_down;
    private double pos;
    private int lane;
    private boolean up, down;

    public IntpDet(double pos, double dx, int lane) {
        this.pos = pos;
        this.lane = lane;
    }

    public void update(Vector act_pos, Vector distances, Vector vel, Vector lanes) {

        up = false;
        down = false;
        int count = (distances.size() < vel.size()) ? distances.size() : vel.size();
        count = count - 1;
        for (int i = 0; i <= count; i++) {
            double posc = ((Double) (act_pos.elementAt(i))).doubleValue();
            int l = ((Integer) (lanes.elementAt(i))).intValue();
            if ((posc > pos) && (l == lane)) {
                down = true;
                pos_down = posc;
                V_down = ((Double) (vel.elementAt(i))).doubleValue();
                dist_down = ((Double) (distances.elementAt(i))).doubleValue();
            }
        }
        // System.out.println("=>"+(new Double(pos_down-pos)).toString());
        for (int i = count; i >= 0; i--) {
            double posc = ((Double) (act_pos.elementAt(i))).doubleValue();
            int l = ((Integer) (lanes.elementAt(i))).intValue();
            if ((posc < pos) && (l == lane)) {
                up = true;
                pos_up = posc;
                dist_up = ((Double) (distances.elementAt(i))).doubleValue();
                V_up = ((Double) (vel.elementAt(i))).doubleValue();
            }
        }
    }

    public double density() {
        double dist = 0.0;
        double density = 0.0;
        if (up == true && down == true) {
            double p1 = (pos_down - pos) / (pos_down - pos_up);
            double p2 = (pos - pos_up) / (pos_down - pos_up);
            dist = (p1 * dist_down) + (p2 * dist_up);
            density = 1.0 / dist;
        } else {
            if (up == true) {
                density = 1.0 / dist_up;
            } else if (down = true) {
                density = 1.0 / dist_down;
            } else {
                density = 0.0;
            }
        }
        return density;
    }

    public double avVel() {
        double vel = 0.0;
        if (up == true && down == true) {

            double p1 = (pos_down - pos) / (pos_down - pos_up);
            double p2 = (pos - pos_up) / (pos_down - pos_up);
            vel = (p1 * V_down) + (p2 * V_up);
        } else {
            if (up == true) {
                vel = V_up;
            } else if (down = true) {
                vel = V_down;
            } else {
                vel = 0.0;
            }
        }
        return vel;
    }
}
