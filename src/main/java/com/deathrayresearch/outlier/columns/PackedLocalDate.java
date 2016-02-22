package com.deathrayresearch.outlier.columns;

import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.chrono.IsoChronology;

/**
 * A short localdate packed into a single int value. It uses a short for year so the range is about +-30,000 years
 * <p>
 * The bytes are packed into the int as:
 * First two bytes: short (year)
 * next byte (month of year)
 * last byte (day of month)
 */
public class PackedLocalDate {

  /**
   * The number of days in a 400 year cycle.
   */
  private static final int DAYS_PER_CYCLE = 146097;
  /**
   * The number of days from year zero to year 1970.
   * There are five 400 year cycles from year zero to 2000.
   * There are 7 leap years from 1970 to 2000.
   */
  static final long DAYS_0000_TO_1970 = (DAYS_PER_CYCLE * 5L) - (30L * 365L + 7L);

  public static byte getDayOfMonth(int date) {
    return (byte) date;  // last byte
  }

  public static short getYear(int date) {
    // get first two bytes, then convert to a short
    byte byte1 = (byte) (date >> 24);
    byte byte2 = (byte) (date >> 16);
    return (short) ((byte2 << 8) + (byte1 & 0xFF));
  }

  public static LocalDate asLocalDate(int date) {
    if (date == Integer.MIN_VALUE) {
      return null;
    }

    // get first two bytes, then each of the other two
    byte yearByte1 = (byte) (date >> 24);
    byte yearByte2 = (byte) (date >> 16);

    return LocalDate.of(
        (short) ((yearByte2 << 8) + (yearByte1 & 0xFF)),
        (byte) (date >> 8),
        (byte) date);
  }

  public static byte getMonthValue(int date) {
    // get the third byte
    return (byte) (date >> 8);
  }

  public static int pack(LocalDate date) {
    short year = (short) date.getYear();
    byte byte1 = (byte) year;
    byte byte2 = (byte) ((year >> 8) & 0xff);
    return Ints.fromBytes(
        byte1,
        byte2,
        (byte) date.getMonthValue(),
        (byte) date.getDayOfMonth());
  }

  public static int pack(short yr, byte m, byte d) {
    byte byte1 = (byte) yr;
    byte byte2 = (byte) ((yr >> 8) & 0xff);
    return Ints.fromBytes(
        byte1,
        byte2,
        m,
        d);
  }

  public static String toDateString(int date) {
    if (date == Integer.MIN_VALUE) {
      return "NA";
    }

    // get first two bytes, then each of the other two
    byte yearByte1 = (byte) (date >> 24);
    byte yearByte2 = (byte) (date >> 16);

    return String.format("%d-%s-%s",
        (short) ((yearByte2 << 8) + (yearByte1 & 0xFF)),
        Strings.padStart(Byte.toString((byte) (date >> 8)), 2, '0'),
        Strings.padStart(Byte.toString((byte) date), 2, '0'));
  }

  public static int getDayOfYear(int packedDate) {
    return getMonth(packedDate).firstDayOfYear(isLeapYear(packedDate)) + getDayOfMonth(packedDate) - 1;
  }

  public static boolean isLeapYear(int packedDate) {
    return IsoChronology.INSTANCE.isLeapYear(getYear(packedDate));
  }

  public static Month getMonth(int packedDate) {
    return Month.of(getMonthValue(packedDate));
  }

  public static int lengthOfMonth(int packedDate) {
    switch (getMonthValue(packedDate)) {
      case 2:
        return (isLeapYear(packedDate) ? 29 : 28);
      case 4:
      case 6:
      case 9:
      case 11:
        return 30;
      default:
        return 31;
    }
  }

  public int lengthOfYear(int packedDate) {
    return (isLeapYear(packedDate) ? 366 : 365);
  }

  /**
   * Returns the epoch day in a form consistent with the java standard
   */
  public static long toEpochDay(int packedDate) {
    long y = PackedLocalDate.getYear(packedDate);
    long m = PackedLocalDate.getMonthValue(packedDate);
    long total = 0;
    total += 365 * y;
    if (y >= 0) {
      total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
    } else {
      total -= y / -4 - y / -100 + y / -400;
    }
    total += ((367 * m - 362) / 12);
    total += getDayOfMonth(packedDate) - 1;
    if (m > 2) {
      total--;
      if (!isLeapYear(packedDate)) {
        total--;
      }
    }
    return total - DAYS_0000_TO_1970;
  }

  public static DayOfWeek getDayOfWeek(int packedDate) {
    int dow0 = (int) Math.floorMod(toEpochDay(packedDate) + 3, 7);
    return DayOfWeek.of(dow0 + 1);
  }

}
