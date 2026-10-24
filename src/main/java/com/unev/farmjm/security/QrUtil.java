package com.unev.farmjm.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.Map;

/** Genera el código QR (ZXing) como imagen de JavaFX. */
public final class QrUtil {

    private QrUtil() { }

    public static WritableImage generar(String texto, int tamano) {
        try {
            BitMatrix m = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamano, tamano,
                    Map.of(EncodeHintType.MARGIN, 1));
            WritableImage img = new WritableImage(m.getWidth(), m.getHeight());
            PixelWriter pw = img.getPixelWriter();
            for (int x = 0; x < m.getWidth(); x++) {
                for (int y = 0; y < m.getHeight(); y++) {
                    pw.setColor(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return img;
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo generar el código QR", e);
        }
    }
}
