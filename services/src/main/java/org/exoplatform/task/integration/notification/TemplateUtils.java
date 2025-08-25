/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.exoplatform.task.integration.notification;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TemplateUtils {
  public static String format(Date date, TimeZone timezone) { 
    if (date == null) {
      return null;
    }
    
    Calendar today = Calendar.getInstance(timezone);
    Calendar cal = Calendar.getInstance(timezone);
    cal.setTime(date);
    String format = "MMM dd yyyy";
    if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
      format = "MMM dd";
    }
    SimpleDateFormat df = new SimpleDateFormat(format);
    df.setTimeZone(timezone);
    return df.format(date);
  }
}
