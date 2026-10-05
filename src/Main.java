import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Main {

    static class LineOp {
        char type;
        byte[] line;

        LineOp(char type, byte[] line) {
            this.type = type;
            this.line = line;
        }
    }

    static class CharOp {
        char type;
        int value;

        CharOp(char type, int value) {
            this.type = type;
            this.value = value;
        }
    }

    static List<byte[]> readLines(String path) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(path));
        List<byte[]> lines = new ArrayList<>();
        int start = 0;

        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n') {
                lines.add(Arrays.copyOfRange(data, start, i));
                start = i + 1;
            }
        }

        if (start < data.length)
            lines.add(Arrays.copyOfRange(data, start, data.length));

        return lines;
    }

    static boolean equalLines(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }

    static List<LineOp> myersLines(List<byte[]> a, List<byte[]> b) {
        List<LineOp> result = new ArrayList<>();
        myersLinesRecursive(a, 0, a.size(), b, 0, b.size(), result);
        return result;
    }

    static void myersLinesRecursive(List<byte[]> a, int aStart, int aEnd, List<byte[]> b, int bStart, int bEnd, List<LineOp> result) {
        while (aStart < aEnd && bStart < bEnd && equalLines(a.get(aStart), b.get(bStart))) {
            result.add(new LineOp(' ', a.get(aStart)));
            aStart++;
            bStart++;
        }

        if (aStart == aEnd) {
            while (bStart < bEnd)
                result.add(new LineOp('+', b.get(bStart++)));
            return;
        }

        if (bStart == bEnd) {
            while (aStart < aEnd)
                result.add(new LineOp('-', a.get(aStart++)));
            return;
        }

        int suffixA = aEnd;
        int suffixB = bEnd;

        while (aStart < suffixA && bStart < suffixB &&
                equalLines(a.get(suffixA - 1), b.get(suffixB - 1))) {
            suffixA--;
            suffixB--;
        }

        if (aStart == suffixA) {
            while (bStart < suffixB)
                result.add(new LineOp('+', b.get(bStart++)));

            while (suffixA < aEnd)
                result.add(new LineOp(' ', a.get(suffixA++)));

            return;
        }

        if (bStart == suffixB) {
            while (aStart < suffixA)
                result.add(new LineOp('-', a.get(aStart++)));

            while (suffixA < aEnd)
                result.add(new LineOp(' ', a.get(suffixA++)));

            return;
        }

        int[] split = middleSnakeLines(a, aStart, suffixA, b, bStart, suffixB);

        int x = split[0];
        int y = split[1];

        myersLinesRecursive(a, aStart, x, b, bStart, y, result);
        myersLinesRecursive(a, x, suffixA, b, y, suffixB, result);

        for (int i = suffixA; i < aEnd; i++)
            result.add(new LineOp(' ', a.get(i)));
    }

    static int[] middleSnakeLines(List<byte[]> a, int aStart, int aEnd, List<byte[]> b, int bStart, int bEnd) {
        int n = aEnd - aStart;
        int m = bEnd - bStart;
        int max = (n + m + 1) / 2;
        int delta = n - m;
        int offset = max + 1;
        int size = 2 * max + 3;

        int[] forward = new int[size];
        int[] backward = new int[size];

        Arrays.fill(forward, -1);
        Arrays.fill(backward, -1);

        forward[offset + 1] = 0;
        backward[offset + 1] = 0;

        boolean odd = (delta & 1) != 0;

        for (int d = 0; d <= max; d++) {
            for (int k = -d; k <= d; k += 2) {
                int index = offset + k;
                int x;

                if (k == -d || (k != d && forward[index - 1] < forward[index + 1]))
                    x = forward[index + 1];
                else
                    x = forward[index - 1] + 1;

                int y = x - k;

                while (x < n && y < m &&
                        equalLines(a.get(aStart + x), b.get(bStart + y))) {
                    x++;
                    y++;
                }

                forward[index] = x;

                if (odd) {
                    int reverseK = delta - k;

                    if (reverseK >= -(d - 1) && reverseK <= d - 1 &&
                            backward[offset + reverseK] != -1) {

                        int backwardX = n - backward[offset + reverseK];

                        if (x >= backwardX)
                            return new int[] {aStart + x, bStart + y};
                    }
                }
            }

            for (int k = -d; k <= d; k += 2) {
                int index = offset + k;
                int x;

                if (k == -d || (k != d && backward[index - 1] < backward[index + 1]))
                    x = backward[index + 1];
                else
                    x = backward[index - 1] + 1;

                int y = x - k;

                while (x < n && y < m &&
                        equalLines(a.get(aEnd - x - 1), b.get(bEnd - y - 1))) {
                    x++;
                    y++;
                }

                backward[index] = x;

                if (!odd) {
                    int forwardK = delta - k;

                    if (forwardK >= -d && forwardK <= d &&
                            forward[offset + forwardK] != -1) {

                        int forwardX = forward[offset + forwardK];
                        int backwardX = n - x;

                        if (forwardX >= backwardX) {
                            return new int[] {
                                aStart + backwardX,
                                bStart + m - (x - k)
                            };
                        }
                    }
                }
            }
        }

        return new int[] {aStart, bStart};
    }

    static List<LineOp> lineDiff(List<byte[]> a, List<byte[]> b) {
        List<LineOp> diff = myersLines(a, b);
        List<LineOp> result = new ArrayList<>();

        int i = 0;

        while (i < diff.size()) {
            if (diff.get(i).type == ' ') {
                result.add(diff.get(i++));
                continue;
            }

            List<LineOp> deletes = new ArrayList<>();
            List<LineOp> inserts = new ArrayList<>();

            while (i < diff.size() && diff.get(i).type != ' ') {
                LineOp op = diff.get(i++);

                if (op.type == '-')
                    deletes.add(op);
                else
                    inserts.add(op);
            }

            result.addAll(deletes);
            result.addAll(inserts);
        }

        return result;
    }

    static void printLines(List<LineOp> diff) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(System.out);

        for (LineOp op : diff) {
            out.write(op.type);
            out.write(op.line);
            out.write('\n');
        }

        out.flush();
    }

    static int[] codePoints(byte[] line) {
        return new String(line, StandardCharsets.UTF_8).codePoints().toArray();
    }

    static List<CharOp> myersChars(int[] a, int[] b) {
        List<CharOp> result = new ArrayList<>();
        myersCharsRecursive(a, 0, a.length, b, 0, b.length, result);
        return result;
    }

    static void myersCharsRecursive(int[] a, int aStart, int aEnd, int[] b, int bStart, int bEnd, List<CharOp> result) {
        while (aStart < aEnd && bStart < bEnd && a[aStart] == b[bStart]) {
            result.add(new CharOp(' ', a[aStart]));
            aStart++;
            bStart++;
        }

        if (aStart == aEnd) {
            while (bStart < bEnd)
                result.add(new CharOp('+', b[bStart++]));
            return;
        }

        if (bStart == bEnd) {
            while (aStart < aEnd)
                result.add(new CharOp('-', a[aStart++]));
            return;
        }

        int suffixA = aEnd;
        int suffixB = bEnd;

        while (aStart < suffixA && bStart < suffixB &&
                a[suffixA - 1] == b[suffixB - 1]) {
            suffixA--;
            suffixB--;
        }

        if (aStart == suffixA) {
            while (bStart < suffixB)
                result.add(new CharOp('+', b[bStart++]));

            while (suffixA < aEnd)
                result.add(new CharOp(' ', a[suffixA++]));

            return;
        }

        if (bStart == suffixB) {
            while (aStart < suffixA)
                result.add(new CharOp('-', a[aStart++]));

            while (suffixA < aEnd)
                result.add(new CharOp(' ', a[suffixA++]));

            return;
        }

        int[] split = middleSnakeChars(a, aStart, suffixA, b, bStart, suffixB);

        int x = split[0];
        int y = split[1];

        myersCharsRecursive(a, aStart, x, b, bStart, y, result);
        myersCharsRecursive(a, x, suffixA, b, y, suffixB, result);

        for (int i = suffixA; i < aEnd; i++)
            result.add(new CharOp(' ', a[i]));
    }

    static int[] middleSnakeChars(int[] a, int aStart, int aEnd, int[] b, int bStart, int bEnd) {
        int n = aEnd - aStart;
        int m = bEnd - bStart;
        int max = (n + m + 1) / 2;
        int delta = n - m;
        int offset = max + 1;
        int size = 2 * max + 3;

        int[] forward = new int[size];
        int[] backward = new int[size];

        Arrays.fill(forward, -1);
        Arrays.fill(backward, -1);

        forward[offset + 1] = 0;
        backward[offset + 1] = 0;

        boolean odd = (delta & 1) != 0;

        for (int d = 0; d <= max; d++) {
            for (int k = -d; k <= d; k += 2) {
                int index = offset + k;
                int x;

                if (k == -d || (k != d && forward[index - 1] < forward[index + 1]))
                    x = forward[index + 1];
                else
                    x = forward[index - 1] + 1;

                int y = x - k;

                while (x < n && y < m && a[aStart + x] == b[bStart + y]) {
                    x++;
                    y++;
                }

                forward[index] = x;

                if (odd) {
                    int reverseK = delta - k;

                    if (reverseK >= -(d - 1) && reverseK <= d - 1 &&
                            backward[offset + reverseK] != -1) {

                        int backwardX = n - backward[offset + reverseK];

                        if (x >= backwardX)
                            return new int[] {aStart + x, bStart + y};
                    }
                }
            }

            for (int k = -d; k <= d; k += 2) {
                int index = offset + k;
                int x;

                if (k == -d || (k != d && backward[index - 1] < backward[index + 1]))
                    x = backward[index + 1];
                else
                    x = backward[index - 1] + 1;

                int y = x - k;

                while (x < n && y < m && a[aEnd - x - 1] == b[bEnd - y - 1]) {
                    x++;
                    y++;
                }

                backward[index] = x;

                if (!odd) {
                    int forwardK = delta - k;

                    if (forwardK >= -d && forwardK <= d &&
                            forward[offset + forwardK] != -1) {

                        int forwardX = forward[offset + forwardK];
                        int backwardX = n - x;

                        if (forwardX >= backwardX) {
                            return new int[] {
                                aStart + backwardX,
                                bStart + m - (x - k)
                            };
                        }
                    }
                }
            }
        }

        return new int[] {aStart, bStart};
    }

    static String formatRanges(List<int[]> ranges) {
        if (ranges.isEmpty())
            return ".";

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < ranges.size(); i++) {
            if (i > 0)
                result.append(',');

            result.append(ranges.get(i)[0]);
            result.append('-');
            result.append(ranges.get(i)[1]);
        }

        return result.toString();
    }

    static String[] highlight(byte[] oldLine, byte[] newLine) {
        int[] oldCp = codePoints(oldLine);
        int[] newCp = codePoints(newLine);

        List<CharOp> ops = myersChars(oldCp, newCp);

        List<int[]> oldRanges = new ArrayList<>();
        List<int[]> newRanges = new ArrayList<>();

        int oldPos = 0;
        int newPos = 0;

        int oldStart = -1;
        int oldEnd = -1;
        int newStart = -1;
        int newEnd = -1;

        for (CharOp op : ops) {
            if (op.type == ' ') {
                if (oldStart != -1) {
                    oldRanges.add(new int[] {oldStart, oldEnd});
                    oldStart = -1;
                }

                if (newStart != -1) {
                    newRanges.add(new int[] {newStart, newEnd});
                    newStart = -1;
                }

                oldPos++;
                newPos++;
            } else if (op.type == '-') {
                if (oldStart == -1)
                    oldStart = oldPos;

                oldPos++;
                oldEnd = oldPos;
            } else {
                if (newStart == -1)
                    newStart = newPos;

                newPos++;
                newEnd = newPos;
            }
        }

        if (oldStart != -1)
            oldRanges.add(new int[] {oldStart, oldEnd});

        if (newStart != -1)
            newRanges.add(new int[] {newStart, newEnd});

        return new String[] {
            formatRanges(oldRanges),
            formatRanges(newRanges)
        };
    }

    static void printHighlight(List<LineOp> diff) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(System.out);
        int i = 0;

        while (i < diff.size()) {
            LineOp current = diff.get(i);

            if (current.type == ' ') {
                out.write(' ');
                out.write(current.line);
                out.write('\n');
                i++;
                continue;
            }

            List<LineOp> deletes = new ArrayList<>();
            List<LineOp> inserts = new ArrayList<>();

            while (i < diff.size() && diff.get(i).type != ' ') {
                LineOp op = diff.get(i++);

                if (op.type == '-')
                    deletes.add(op);
                else
                    inserts.add(op);
            }

            for (LineOp op : deletes) {
                out.write('-');
                out.write(op.line);
                out.write('\n');
            }

            for (int j = 0; j < inserts.size(); j++) {
                LineOp op = inserts.get(j);

                out.write('+');
                out.write(op.line);
                out.write('\n');

                if (j < deletes.size()) {
                    String[] ranges = highlight(deletes.get(j).line, op.line);

                    out.write('?');
                    out.write(' ');
                    out.write(ranges[0].getBytes(StandardCharsets.US_ASCII));
                    out.write(' ');
                    out.write('|');
                    out.write(' ');
                    out.write(ranges[1].getBytes(StandardCharsets.US_ASCII));
                    out.write('\n');
                }
            }
        }

        out.flush();
    }

    public static void main(String[] args) {
        if (args.length != 3 ||
                (!args[0].equals("lines") && !args[0].equals("highlight"))) {
            System.err.println("usage: Main lines|highlight A_PATH B_PATH");
            System.exit(2);
        }

        String command = args[0];
        String aPath = args[1];
        String bPath = args[2];

        List<byte[]> fileA;
        List<byte[]> fileB;

        try {
            fileA = readLines(aPath);
            fileB = readLines(bPath);
        } catch (IOException e) {
            System.err.println("error: cannot read input file: " + e.getMessage());
            System.exit(2);
            return;
        }

        try {
            List<LineOp> diff = lineDiff(fileA, fileB);

            if (command.equals("lines"))
                printLines(diff);
            else
                printHighlight(diff);

        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
            System.exit(2);
        }
    }
}