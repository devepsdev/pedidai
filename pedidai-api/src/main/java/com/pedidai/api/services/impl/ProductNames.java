package com.pedidai.api.services.impl;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Normalització de noms de producte i unitats per poder comparar el mateix producte
 * entre proveïdors ("TOMÀQUET PERA" i "Tomate pera" han de caure al mateix grup només
 * si la IA o l'usuari els ha donat el mateix nom genèric; aquí només s'unifica el format).
 */
public final class ProductNames {

    private static final Map<String, String> UNITS = Map.ofEntries(
            Map.entry("kg", "kg"), Map.entry("kgs", "kg"), Map.entry("kilo", "kg"), Map.entry("kilos", "kg"),
            Map.entry("kilogramo", "kg"), Map.entry("kilogramos", "kg"), Map.entry("quilo", "kg"), Map.entry("quilos", "kg"),
            Map.entry("g", "g"), Map.entry("gr", "g"), Map.entry("grs", "g"), Map.entry("gramo", "g"), Map.entry("gramos", "g"),
            Map.entry("l", "l"), Map.entry("lt", "l"), Map.entry("lts", "l"), Map.entry("litro", "l"), Map.entry("litros", "l"),
            Map.entry("litre", "l"), Map.entry("litres", "l"),
            Map.entry("ml", "ml"), Map.entry("cl", "cl"),
            Map.entry("u", "ud"), Map.entry("ud", "ud"), Map.entry("uds", "ud"), Map.entry("un", "ud"), Map.entry("und", "ud"),
            Map.entry("unidad", "ud"), Map.entry("unidades", "ud"), Map.entry("unitat", "ud"), Map.entry("unitats", "ud"),
            Map.entry("caja", "caja"), Map.entry("cajas", "caja"), Map.entry("cj", "caja"), Map.entry("caixa", "caja"),
            Map.entry("caixes", "caja"),
            Map.entry("docena", "docena"), Map.entry("dotzena", "docena"),
            Map.entry("garrafa", "garrafa"), Map.entry("garrafas", "garrafa"), Map.entry("garrafes", "garrafa"),
            Map.entry("botella", "botella"), Map.entry("botellas", "botella"), Map.entry("ampolla", "botella"),
            Map.entry("ampolles", "botella"),
            Map.entry("paquete", "paquete"), Map.entry("paquetes", "paquete"), Map.entry("paquet", "paquete"),
            Map.entry("paquets", "paquete"), Map.entry("pack", "paquete"),
            Map.entry("bandeja", "bandeja"), Map.entry("bandejas", "bandeja"), Map.entry("safata", "bandeja"),
            Map.entry("saco", "saco"), Map.entry("sacos", "saco"), Map.entry("sac", "saco"),
            Map.entry("lata", "lata"), Map.entry("latas", "lata"), Map.entry("llauna", "lata"),
            Map.entry("barril", "barril"), Map.entry("barriles", "barril")
    );

    private ProductNames() {
    }

    /** Nom genèric per mostrar: minúscules i espais simples, conservant els accents ("limón"). */
    public static String generic(String name) {
        if (name == null) {
            return null;
        }
        String s = name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return s.isEmpty() ? null : (s.length() > 255 ? s.substring(0, 255) : s);
    }

    /** Minúscules, sense accents ni signes de puntuació i amb espais simples. */
    public static String canonical(String name) {
        if (name == null) {
            return null;
        }
        String s = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9ñç]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return s.isEmpty() ? null : s;
    }

    /**
     * Arrel aproximada per comparar singular i plural en castellà i català:
     * "tomates"/"tomate" → "tomat", "limones"/"limon" → "limon", "aguas"/"agua" → "agua".
     */
    public static String singular(String word) {
        if (word == null || word.length() <= 3) {
            return word;
        }
        String w = word;
        if (w.endsWith("s")) {
            w = w.substring(0, w.length() - 1);
        }
        if (w.length() > 3 && w.endsWith("e")) {
            w = w.substring(0, w.length() - 1);
        }
        return w;
    }

    /** Unitat normalitzada ("kilos" → "kg", "uds" → "ud"...). Si no es reconeix, la mateixa en minúscules. */
    public static String unit(String unit) {
        String c = canonical(unit);
        if (c == null) {
            return "";
        }
        return UNITS.getOrDefault(c, UNITS.getOrDefault(c.replace(" ", ""), c));
    }
}
