package org.pucmm.eventos.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

public class QRUtil {
    private QRUtil() {}

    /**
     * Encodes {@code content} as a 300×300 PNG QR code and returns it as a
     * base64 data URI ready to be embedded in an {@code <img>} tag.
     */
    public static String generateQRBase64(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    300, 300,
                    Map.of(
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, 2,
                            EncodeHintType.CHARACTER_SET, "UTF-8"
                    )
            );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (WriterException | IOException e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }
}