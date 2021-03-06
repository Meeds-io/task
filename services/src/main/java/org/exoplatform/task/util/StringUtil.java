/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.util;

import java.util.regex.Pattern;

import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.gatein.common.text.EntityEncoder;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class StringUtil {
  public static final String  ORDERBY_DATE  = "date" ;
  public static final String  ORDERBY_TITLE  = "title" ;  
  public static final String ORDERBY_RELEVANCY = "relevancy";
  public static final String ASC = "asc";
  public static final String DESC = "desc";  
  
  public static final  Pattern FUZZY = Pattern.compile(".[~][0]([\\.][0-9])");
  
  private static final PolicyFactory htmlPolicy = new HtmlPolicyBuilder().allowCommonInlineFormattingElements()
      .allowElements("cite", "q", "pre", "img", "p").allowWithoutAttributes("span").allowAttributes("class").onElements("div", "p", "span", "a", "pre", "img").toFactory()
      .and(Sanitizers.LINKS).and(Sanitizers.BLOCKS).and(Sanitizers.IMAGES).and(Sanitizers.STYLES).and(Sanitizers.TABLES);
  
  public static String highlight(String text, String keyword, String before, String after) {
    if (text == null || text.isEmpty() || keyword == null || keyword.isEmpty()) {
      return text;
    }
    String lowerText = text.toLowerCase();
    String lowerKeyword = keyword.toLowerCase();
    StringBuilder result = new StringBuilder();

    EntityEncoder encoder = HTMLEntityEncoder.getInstance();
    int length = keyword.length();
    int fromIndex = 0;
    int index = lowerText.indexOf(lowerKeyword, fromIndex);
    while(index != -1) {
      result.append(encoder.encode(text.substring(fromIndex, index)));
      result.append(before);
      result.append(encoder.encode(text.substring(index, index + length)));
      result.append(after);
      fromIndex = index + length;
      index = lowerText.indexOf(lowerKeyword, fromIndex);
    }
    result.append(encoder.encode(text.substring(fromIndex)));
    return result.toString();
  }
  
  public static String encodeInjectedHtmlTag(String str) {
    if (str != null && !str.isEmpty()) {
      return htmlPolicy.sanitize(str);
    }
    return str;
  }  
}