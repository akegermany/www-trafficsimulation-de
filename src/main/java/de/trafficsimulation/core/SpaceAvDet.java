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

public class SpaceAvDet {

    private Vector avVel = new Vector();
    private Vector avDist = new Vector();
    private double DeltaX;
    private double pos;
    private int lane;

    public SpaceAvDet(double pos, double DeltaX, int lane) {
        this.pos = pos;
        this.DeltaX = DeltaX;
        this.lane = lane;
    }

    public void update(Vector act_pos, Vector distances, Vector vel, Vector lanes) {

        int count = (distances.size() < vel.size()) ? distances.size() : vel.size();
        count = count - 1;
        avVel = new Vector();
        avDist = new Vector();
        for (int i = 0; i <= count; i++) {
            if ((((Integer) (lanes.elementAt(i))).intValue()) == lane) {
                double posc = ((Double) (act_pos.elementAt(i))).doubleValue();
                if ((posc > (pos - DeltaX)) && (posc < (pos + DeltaX))) {
                    avVel.insertElementAt((vel.elementAt(i)), 0);
                    avDist.insertElementAt((distances.elementAt(i)), 0);
                }
            }
        }
    }

    public double density() {
        double dist = 0.0;
        double density = 0.0;
        int count = avDist.size();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                dist = dist + ((Double) (avDist.elementAt(i))).doubleValue();
            }
            density = ((double) (count)) / dist;
        }
        return density;
    }

    public double avVel() {
        double vel = 0.0;
        int count = avVel.size();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                vel = vel + ((Double) (avVel.elementAt(i))).doubleValue();
            }
            vel = vel / count;
        }
        return vel;
    }
}
