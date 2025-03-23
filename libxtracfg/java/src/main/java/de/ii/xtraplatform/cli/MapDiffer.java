/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapDiffer {

  public static Map<String, String> diff(
      Map<String, Object> source, Map<String, Object> other, boolean ignoreSourceMissing) {
    Map<String, String> result = new LinkedHashMap<>();

    source.forEach(
        (key, value) -> {
          if (!other.containsKey(key)) {
            result.put(key, "other missing");
          } else if (Objects.equals(value, other.get(key))) {
            return;
          } else if (value instanceof Map && other.get(key) instanceof Map) {
            result.putAll(
                diff(
                    (Map<String, Object>) value,
                    (Map<String, Object>) other.get(key),
                    ignoreSourceMissing));
          } else if (value instanceof List && other.get(key) instanceof List) {
            result.putAll(
                diff(
                    (List<Object>) value, (List<Object>) other.get(key), key, ignoreSourceMissing));
          } else if (value instanceof List && other.get(key) instanceof Map) {
            result.putAll(
                diff(
                    (List<Object>) value,
                    (Map<String, Object>) other.get(key),
                    false,
                    ignoreSourceMissing));
          } else if (value instanceof Map && other.get(key) instanceof List) {
            result.putAll(
                diff(
                    (List<Object>) other.get(key),
                    (Map<String, Object>) value,
                    true,
                    ignoreSourceMissing));
          } else {
            result.put(key, String.format("'%s' != '%s'", value, other.get(key)));
          }
        });

    other.forEach(
        (key, value) -> {
          if (!ignoreSourceMissing && !source.containsKey(key)) {
            result.put(key, "source missing");
          }
        });

    return result;
  }

  private static Map<String, String> diff(
      List<Object> source, List<Object> other, String parentKey, boolean ignoreSourceMissing) {

    Map<String, String> result = new LinkedHashMap<>();

    for (int i = 0; i < source.size(); i++) {
      String key = String.format("%s[%d]", parentKey, i);
      Object value = source.get(i);

      if (other.size() < i + 1) {
        result.put(key, "other missing");
      } else if (Objects.equals(value, other.get(i))) {
        continue;
      } else if (value instanceof Map && other.get(i) instanceof Map) {
        result.putAll(
            diff(
                (Map<String, Object>) value,
                (Map<String, Object>) other.get(i),
                ignoreSourceMissing));
      } else if (value instanceof List && other.get(i) instanceof List) {
        result.putAll(
            diff((List<Object>) value, (List<Object>) other.get(i), key, ignoreSourceMissing));
      } else if (value instanceof List && other.get(i) instanceof Map) {
        result.putAll(
            diff(
                (List<Object>) value,
                (Map<String, Object>) other.get(i),
                false,
                ignoreSourceMissing));
      } else if (value instanceof Map && other.get(i) instanceof List) {
        result.putAll(
            diff(
                (List<Object>) other.get(i),
                (Map<String, Object>) value,
                true,
                ignoreSourceMissing));
      } else {
        result.put(key, String.format("'%s' != '%s'", value, other.get(i)));
      }
    }

    for (int i = source.size(); i < other.size(); i++) {
      String key = String.format("%s[%d]", parentKey, i);
      if (!ignoreSourceMissing) {
        result.put(key, "source missing");
      }
    }

    return result;
  }

  private static Map<String, String> diff(
      List<Object> source,
      Map<String, Object> other,
      boolean reverse,
      boolean ignoreSourceMissing) {
    Map<String, String> result = new LinkedHashMap<>();

    if (source.size() == 1 && source.get(0) instanceof Map) {
      return reverse
          ? diff(other, (Map<String, Object>) source.get(0), ignoreSourceMissing)
          : diff((Map<String, Object>) source.get(0), other, ignoreSourceMissing);
    }

    return result;
  }
}
