package edu.pucmm.icc352.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.util.Map;

public class QRGenerator {

    public static byte[] generate(String content, int width, int height) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.CHARACTER_SET, "UTF-8",
                EncodeHintType.MARGIN, 1
        );
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}