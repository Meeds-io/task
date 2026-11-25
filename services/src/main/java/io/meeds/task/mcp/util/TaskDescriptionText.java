/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.task.mcp.util;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * The description of a task as the plain text a compact list item carries:
 * the stored HTML read as text, one line per block, without resolving its
 * links or mentions, and cut to a bounded length.
 */
public final class TaskDescriptionText {

  /**
   * The most characters a compact list item carries of a task's description;
   * a longer one is cut at the last word or line boundary before it.
   */
  public static final int MAX_LENGTH = 500;

  /**
   * Utility class.
   */
  private TaskDescriptionText() {
  }

  /**
   * Reads an HTML description as plain text: the entities decoded, the tags
   * removed, the text of a link or a mention kept as written, a line break
   * after each block element (paragraph, list item, heading, table row…) and
   * each {@code <br>}, the spaces of each line collapsed, and the blank lines
   * dropped.
   *
   * @param html a description, possibly null or blank
   * @return its plain text, null when it holds no text
   */
  public static String toPlainText(String html) {
    if (StringUtils.isBlank(html)) {
      return null;
    }
    StringBuilder text = new StringBuilder();
    NodeTraversor.traverse(new LineBreakingVisitor(text), Jsoup.parseBodyFragment(html).body());
    String plainText = Arrays.stream(text.toString().split("\n"))
                             .map(line -> line.replaceAll("\\h+", " ").strip())
                             .filter(line -> !line.isEmpty())
                             .collect(Collectors.joining("\n"));
    return plainText.isEmpty() ? null : plainText;
  }

  /**
   * Cuts a text to {@link #MAX_LENGTH} characters at most: at the last
   * whitespace (a line break or a space) at or before that length, so that no
   * word is cut, or, for a text without any, at that length, never between
   * the two halves of a surrogate pair. A text that fits is returned as is.
   *
   * @param text a text, possibly null
   * @return the text, cut when longer than {@link #MAX_LENGTH}
   */
  public static String cut(String text) {
    if (text == null || text.length() <= MAX_LENGTH) {
      return text;
    }
    int end = MAX_LENGTH;
    if (!Character.isWhitespace(text.charAt(end))) {
      int boundary = lastWhitespace(text, end);
      if (boundary > 0) {
        end = boundary;
      } else if (Character.isHighSurrogate(text.charAt(end - 1))) {
        end--;
      }
    }
    return text.substring(0, end).stripTrailing();
  }

  /**
   * @param text a text
   * @param end an index of the text
   * @return the index of the last whitespace before {@code end}, -1 for none
   */
  private static int lastWhitespace(String text, int end) {
    for (int i = end - 1; i > 0; i--) {
      if (Character.isWhitespace(text.charAt(i))) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Appends the text of the nodes it visits, with a line break at each
   * {@code <br>} and after each block element.
   */
  private static final class LineBreakingVisitor implements NodeVisitor {

    /**
     * The text the visited nodes are appended to.
     */
    private final StringBuilder text;

    /**
     * @param output the text the visited nodes are appended to
     */
    private LineBreakingVisitor(StringBuilder output) {
      this.text = output;
    }

    /**
     * Appends the text of a text node, its whitespace normalised, and a line
     * break for a {@code <br>}.
     *
     * @param node the visited node
     * @param depth its depth
     */
    @Override
    public void head(Node node, int depth) {
      if (node instanceof TextNode textNode) {
        text.append(textNode.text());
      } else if (node instanceof Element element && element.nameIs("br")) {
        text.append('\n');
      }
    }

    /**
     * Appends a line break after a block element.
     *
     * @param node the visited node
     * @param depth its depth
     */
    @Override
    public void tail(Node node, int depth) {
      if (node instanceof Element element && element.isBlock()) {
        text.append('\n');
      }
    }
  }

}
