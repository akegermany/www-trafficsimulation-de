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

public class IDMCar extends IDM implements MicroModel, Constants {

    public IDMCar() {
        // System.out.println("in Cstr of class IDMCar (no own ve calc)");

        v0 = V0_INIT_KMH / 3.6;
        delta = 4.0;
        a = A_INIT_CAR_MSII; // 1
        b = B_INIT_MSII; // 1.0
        s0 = S0_INIT_M;
        T = T_INIT_S; // 1.5
        sqrtab = Math.sqrt(a * b);
        initialize();
    }
}
