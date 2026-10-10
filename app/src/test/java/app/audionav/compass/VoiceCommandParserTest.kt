/*
 * Copyright (C) 2026 Michael Whapples
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 */
package app.audionav.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandParserTest {
    @Test
    fun parsesSignedCourseChanges() {
        assertEquals(-12, VoiceCommandParser.parseCourseDelta("Change course by -12 degrees"))
        assertEquals(12, VoiceCommandParser.parseCourseDelta("change course by +12 degrees"))
        assertEquals(12, VoiceCommandParser.parseCourseDelta("Change course by 12 degrees"))
        assertEquals(-12, VoiceCommandParser.parseCourseDelta("Change course -12 degrees"))
        assertEquals(12, VoiceCommandParser.parseCourseDelta("Change course +12 degrees"))
    }

    @Test
    fun acceptsInclusiveBounds() {
        assertEquals(-180, VoiceCommandParser.parseCourseDelta("Change course by -180 degrees"))
        assertEquals(180, VoiceCommandParser.parseCourseDelta("Change course by +180 degrees"))
        assertEquals(-180, VoiceCommandParser.parseCourseDelta("Change course -180 degrees"))
        assertEquals(180, VoiceCommandParser.parseCourseDelta("Change course +180 degrees"))
    }

    @Test
    fun rejectsMalformedAndOutOfRangeCommands() {
        assertNull(VoiceCommandParser.parseCourseDelta("Change course by 12"))
        assertNull(VoiceCommandParser.parseCourseDelta("Change course 12"))
        assertNull(VoiceCommandParser.parseCourseDelta("Change course by 1.5 degrees"))
        assertNull(VoiceCommandParser.parseCourseDelta("Change course by 181 degrees"))
        assertNull(VoiceCommandParser.parseCourseDelta("Change course by -181 degrees"))
        assertNull(VoiceCommandParser.parseCourseDelta("Change course 181 degrees"))
        assertNull(VoiceCommandParser.parseCourseDelta("Change course by 999999999999999999999 degrees"))
    }
}