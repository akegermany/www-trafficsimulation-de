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

import java.awt.Color;

public class Obstacle implements Moveable {

    private double vel = 0.0;
    private double pos;
    private int lane;
    private double length;
    private Color color = Color.white;
    private int nr; // with nr possible to overwrite default color
                    // in SimCanvas, methods paint*

    // model necessary since obstacle=type "Moveable" and lane change checks
    // acceleration! influences 3 following locations labeld with xxx

    public MicroModel model; // xxx

    public MicroModel model() {
        return model;
    } // xxx

    public Obstacle(double x, int lane, double length, int nr) {
        pos = x;
        this.lane = lane;
        this.length = length;
        this.nr = nr;
        this.model = new IDMCar(); // xxx
    }

    public void setPosition(double x) {
        pos = x;
    }

    public void setVelocity(double v) {
        vel = v;
    } // includes moving obstacle

    public void setLane(int lane) {
        this.lane = lane;
    }

    public void setLaneLast(int lane) {
        this.lane = lane;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public void setColor(Color color) {
    }

    public void setModel(MicroModel model) {
    }

    public void setLaneChange(LaneChange lanechange) {
    }

    public double position() {
        return pos;
    }

    public double velocity() {
        return vel;
    }

    public int lane() {
        return lane;
    }

    public double realLane() {
        return lane;
    }

    public double length() {
        return length;
    }

    public Color color() {
        return color;
    }

    public int NR() {
        return nr;
    }

    public boolean timeToChange(double dt) {
        return false;
    }

    public boolean change(Moveable fOld, Moveable fNew, Moveable bNew) {
        return false;
    }

    public double dAcc(Moveable vwd, Moveable bwd) {
        return 0.0;
    }

    public void translate(double dt) {
    }

    public void accelerate(double dt, Moveable vwd) {
    }

    public double acceleration(Moveable vwd) {
        return 0.0;
    }
}
