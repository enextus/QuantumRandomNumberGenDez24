package org.ThreeDotsSierpinski;

import org.ThreeDotsSierpinski.RNProvider;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class KolmogorovTest {
    public static void main(String[] args) throws IOException {
        RNProvider rnProvider = new RNProvider();
        String hexData = "1A2B3C4D5E6F7A8B9C0D"; // Пример HEX-данных
        List<Integer> numbers = rnProvider.getTrueRandomNumbersFromHex(hexData);

        // Записываем числа в файл
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("random.txt"))) {
            for (int num : numbers) {
                writer.write(num + "\n");
            }
        }

        // Сжимаем файл
        File inputFile = new File("random.txt");
        File compressedFile = new File("random.txt.gz");
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(compressedFile);
             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
        }

        // Сравниваем размеры
        long originalSize = inputFile.length();
        long compressedSize = compressedFile.length();
        double compressionRatio = (double) compressedSize / originalSize;
        System.out.println("Original size: " + originalSize + " bytes");
        System.out.println("Compressed size: " + compressedSize + " bytes");
        System.out.println("Compression ratio: " + compressionRatio);
    }
}