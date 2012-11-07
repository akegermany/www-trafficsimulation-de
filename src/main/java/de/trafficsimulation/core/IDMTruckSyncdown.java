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

// Trucks in downstream bottleneck region of potential synchr. traffic

public class IDMTruckSyncdown extends IDM implements MicroModel {

    public IDMTruckSyncdown() {
        System.out.println("in Cstr of IDMTruckSyncdown (no own ve calc)");

        v0 = 30 / 3.6;
        delta = 4.0;
        a = 0.1;
        b = 2;
        s0 = 2.0;
        T = 2.0;
        sqrtab = Math.sqrt(a * b);
        initialize();
    }
}
