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

public class BCCar implements Moveable {

    private double pos;
    private double vel = 10.0;
    private int lane = 0;
    private Color color = Color.red;
    private double length;
    private MicroModel model;

    public BCCar(double x, double v, int lane, MicroModel model, double length) {
        pos = x;
        vel = v;
        this.lane = lane;
        this.model = model;
        this.length = length;
    }

    public void setPosition(double x) {
        pos = x;
    }

    public void setVelocity(double v) {
        vel = v;
    }

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
        this.color = color;
    }

    public void setModel(MicroModel model) {
        this.model = model;
    }

    public void setLaneChange(LaneChange lanechange) {
    }

    public double position() {
        return pos;
    }

    public int lane() {
        return lane;
    }

    public double realLane() {
        return lane;
    }

    public double velocity() {
        return vel;
    }

    public MicroModel model() {
        return model;
    }

    public double length() {
        return length;
    }

    public Color color() {
        return Color.white;
    }

    public int NR() {
        return 0;
    }

    public boolean timeToChange(double dt) {
        return false;
    }

    public double dAcc(Moveable vwd, Moveable bwd) {
        return 0.0;
    }

    public boolean change(Moveable fOld, Moveable fNew, Moveable bNew) {
        return false;
    }

    public void translate(double dt) {
    }

    public void accelerate(double dt, Moveable vwd) {
    }

    public double acceleration(Moveable vwd) {
        return 0.0;
    }
}
