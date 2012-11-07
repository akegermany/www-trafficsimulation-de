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

/**
 * Moveable represents a general vehicle object with position, velocity eyc.
 * In each time step, the objects are updated by moving them forward
 * (method translate), by changing the velocity (method accelerate), and, in time steps where timeToChange return true, by
 * possibly changing the lane (method change) <br>
 * <br>
 * Implementations of this interface include
 * <ul>
 * <li>Car: all "normal" cars and trucks
 * <li>Obstacle: These are implemented by immobile vehicles
 * <li>BCCar: These are introduced for bookkeeping purposes, only to make sure that any Car has a predecessor required for calculating the
 * acceleration.
 * </ul>
 */

public interface Moveable {

    public void setPosition(double x);

    public void setVelocity(double v);

    public void setLane(int lane);

    public void setLaneLast(int lane);

    public void setModel(MicroModel model);

    public void setLaneChange(LaneChange lanechange);

    public void setLength(double length);

    public void setColor(Color color);

    public double position();

    public double velocity();

    public int lane();

    public double realLane();

    public MicroModel model();

    public double length();

    public Color color();

    public int NR();

    public boolean change(Moveable fOld, Moveable fNew, Moveable bNew);

    public boolean timeToChange(double dt);

    public double dAcc(Moveable vwd, Moveable bwd);

    public void translate(double dt);

    public void accelerate(double dt, Moveable vwd);

    public double acceleration(Moveable vwd);
}
