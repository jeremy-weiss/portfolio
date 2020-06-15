// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Collection;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<String> attendees = request.getAttendees();

    // Find times that don't work
    ArrayList<TimeRange> badRanges = new ArrayList<>();
    for (Event event : events) {
      Collection<String> eventAttendees = event.getAttendees();
      for (String attendee : attendees) {
        if (eventAttendees.contains(attendee)) {
          badRanges.add(event.getWhen());
          break;
        }
      }
    }
    badRanges.sort(TimeRange.ORDER_BY_START);

    // Prune time ranges
    for (int i = 0; i < badRanges.size() - 1; i++) {
      TimeRange range1 = badRanges.get(i);
      TimeRange range2 = badRanges.get(i + 1);
      // Range2 can contain range1 if they have equal start times, but that case is equivalent to
      // overlap
      if (range1.overlaps(range2)) {
        if (range1.contains(range2)) {
          // Irrelevant time range
          badRanges.set(i + 1, null);
        } else {
          // Merge overlapping
          TimeRange combined = TimeRange.fromStartEnd(range1.start(), range2.end(), false);
          badRanges.set(i, combined);
        }
      }
    }

    ArrayList<TimeRange> validRanges = new ArrayList<>();
    long duration = request.getDuration();
    int startTime = TimeRange.START_OF_DAY;
    int endTime;
    for (TimeRange range : badRanges) {
      if (range == null) {
        continue;
      }
      endTime = range.start();
      addIfAvailable(validRanges, startTime, endTime, duration);
      startTime = range.end();
    }
    addIfAvailable(validRanges, startTime, TimeRange.END_OF_DAY + 1, duration);

    return validRanges;
  }

  // Adds a time range to the collection if a meeting can be held in the specified time
  private void addIfAvailable(
      Collection<TimeRange> validRanges, int startTime, int endTime, long duration) {
    if (endTime - startTime >= duration) {
      TimeRange validRange = TimeRange.fromStartEnd(startTime, endTime, false);
      validRanges.add(validRange);
    }
  }
}
