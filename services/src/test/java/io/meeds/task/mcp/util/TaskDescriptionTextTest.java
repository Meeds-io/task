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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The plain text and the cut of a task description in a compact list item.
 */
public class TaskDescriptionTextTest {

  /**
   * The tags and entities are removed, a link and a mention are kept as their
   * text, each block and line break ends a line, the spaces of a line are
   * collapsed and the blank lines dropped. Mutants: the HTML kept; one line
   * for the whole text; the entities left encoded.
   */
  @Test
  public void anHtmlDescriptionIsReadAsOneLinePerBlock() {
    String html = "<div><h3>Deal</h3><p>Contact:&nbsp; <a href=\"/portal/dw/profile/john\">@john</a>  &lt;CTO&gt;</p>"
        + "<p>&nbsp;</p><ol><li>Users: 500</li><li>Size: 50&nbsp;k&euro;</li></ol>Planning: Q1<br/>Signature: Q2</div>";

    assertEquals("Deal\nContact: @john <CTO>\nUsers: 500\nSize: 50 k€\nPlanning: Q1\nSignature: Q2",
                 TaskDescriptionText.toPlainText(html));
  }

  /**
   * A description without text has no plain text. Mutant: an empty string
   * returned, which the item would serialise as an empty description.
   */
  @Test
  public void aDescriptionWithoutTextHasNone() {
    assertNull(TaskDescriptionText.toPlainText(null));
    assertNull(TaskDescriptionText.toPlainText("  "));
    assertNull(TaskDescriptionText.toPlainText("<p>&nbsp;</p><p><br></p>"));
  }

  /**
   * A text of 500 characters at most is returned as is. Mutant: the bound
   * off by one.
   */
  @Test
  public void aTextThatFitsIsNotCut() {
    String text = "a".repeat(499) + " ";
    assertSame(text, TaskDescriptionText.cut(text));
    assertNull(TaskDescriptionText.cut(null));
  }

  /**
   * A longer text is cut at its last whitespace, a line break or a space, at
   * or before 500 characters, so that no word is cut; when the 501st
   * character is a whitespace, at 500. Mutants: a hard cut inside a word; a
   * cut after 500 characters.
   */
  @Test
  public void aLongTextIsCutAtAWordOrLineBoundary() {
    String words = "abcdefgh ".repeat(60);
    String cut = TaskDescriptionText.cut(words);
    assertEquals("abcdefgh ".repeat(55).strip(), cut);

    String lines = "x".repeat(495) + "\nlonger-than-the-rest";
    assertEquals("x".repeat(495), TaskDescriptionText.cut(lines));

    String exact = "y".repeat(500) + " tail";
    assertEquals("y".repeat(500), TaskDescriptionText.cut(exact));
  }

  /**
   * A text without any whitespace is cut at 500 characters, never between
   * the two halves of a surrogate pair. Mutant: the pair split, leaving a
   * lone high surrogate.
   */
  @Test
  public void aTextWithoutWhitespaceIsNeverCutInsideASurrogatePair() {
    String plain = "z".repeat(600);
    assertEquals(500, TaskDescriptionText.cut(plain).length());

    String emoji = "z".repeat(499) + "😀" + "z".repeat(50);
    String cut = TaskDescriptionText.cut(emoji);
    assertEquals(499, cut.length());
    assertTrue(Character.isLowSurrogate(cut.charAt(cut.length() - 1)) || !Character.isHighSurrogate(cut.charAt(cut.length() - 1)));
  }

}
