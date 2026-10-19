package com.fedex.tracking.util;

/** Convierte entre largo/ancho/alto (como los maneja el front) y el texto "LxAxH" que se guarda en BD. */
public final class DimensionesUtil {

    private DimensionesUtil() {
    }

    public static String formatear(Double largo, Double ancho, Double alto) {
        return numero(largo) + "x" + numero(ancho) + "x" + numero(alto);
    }

    /** @return {largo, ancho, alto} o null si el texto no tiene ese formato. */
    public static Double[] parsear(String dimensiones) {
        if (dimensiones == null || dimensiones.isBlank()) return null;
        String limpio = dimensiones.trim().toLowerCase().replace("×", "x").replace("*", "x")
                .replace("cm", "").replaceAll("\\s", "");
        String[] partes = limpio.split("x");
        if (partes.length != 3) return null;
        try {
            return new Double[]{
                    Double.valueOf(partes[0].replace(',', '.')),
                    Double.valueOf(partes[1].replace(',', '.')),
                    Double.valueOf(partes[2].replace(',', '.'))
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String numero(Double valor) {
        double v = valor;
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
