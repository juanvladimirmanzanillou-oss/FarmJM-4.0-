package com.unev.farmjm.dao;

import com.unev.farmjm.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Recetas de ejemplo para la pantalla de Recetas / Controlados (módulo aún sin tabla propia en la interfaz). */
public class DemoData {

    public static List<Receta> recetas(List<Medicamento> meds) {
        if (meds.size() < 4) return new ArrayList<>();   // sin datos suficientes
        Medico dr = new Medico(1, "Dra. Laura Méndez", "Exequátur 11245", "Medicina general");
        Medico dr2 = new Medico(2, "Dr. Carlos Peña", "Exequátur 09877", "Psiquiatría");
        Cliente c1 = new Cliente(1, "María Pérez", "001-0000001-1", "809-555-0111", "");
        Cliente c2 = new Cliente(2, "José Martínez", "001-0000002-2", "809-555-0222", "");
        List<Receta> l = new ArrayList<>();
        Receta r1 = new Receta(1001, dr, c1, LocalDate.now().minusDays(1), false);
        r1.agregarDetalle(new DetalleReceta(1, meds.get(0), 21, "1 cápsula cada 8 horas por 7 días"));
        Receta r2 = new Receta(1002, dr2, c2, LocalDate.now(), true);
        r2.agregarDetalle(new DetalleReceta(2, meds.get(3), 30, "1 tableta cada noche"));
        l.add(r1);
        l.add(r2);
        return l;
    }
}
