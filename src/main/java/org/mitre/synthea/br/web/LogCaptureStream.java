package org.mitre.synthea.br.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures stdout/stderr tail for the web UI while forwarding to the original stream.
 */
final class LogCaptureStream extends OutputStream {

  private static final int MAX_LINES = 50;
  private static final Pattern RECORDS_TOTAL =
      Pattern.compile("Records: total=(\\d+),");

  private final PrintStream delegate;
  private final Deque<String> lines = new ArrayDeque<>();
  private final StringBuilder currentLine = new StringBuilder();
  private volatile int parsedTotal = -1;

  LogCaptureStream(PrintStream delegate) {
    this.delegate = delegate;
  }

  @Override
  public void write(int b) throws IOException {
    // Generator may run patient generation on a thread pool, so multiple threads can write
    // to System.out concurrently while this stream is installed; ArrayDeque/StringBuilder are
    // not thread-safe, and the HTTP status thread reads via tail() concurrently.
    delegate.write(b);
    synchronized (this) {
      if (b == '\n') {
        flushLine();
      } else if (b != '\r') {
        currentLine.append((char) b);
      }
    }
  }

  private void flushLine() {
    if (currentLine.length() == 0) {
      return;
    }
    String line = currentLine.toString();
    currentLine.setLength(0);
    lines.addLast(line);
    while (lines.size() > MAX_LINES) {
      lines.removeFirst();
    }
    Matcher matcher = RECORDS_TOTAL.matcher(line);
    if (matcher.find()) {
      try {
        parsedTotal = Integer.parseInt(matcher.group(1));
      } catch (NumberFormatException ex) {
        // Ignore unparsable totals; progress simply stays at its last known value.
      }
    }
  }

  synchronized List<String> tail() {
    if (currentLine.length() > 0) {
      flushLine();
    }
    return new ArrayList<>(lines);
  }

  int parsedTotal() {
    return parsedTotal;
  }

  synchronized void reset() {
    lines.clear();
    currentLine.setLength(0);
    parsedTotal = -1;
  }

  PrintStream asPrintStream() {
    return new PrintStream(this, true, StandardCharsets.UTF_8);
  }
}
