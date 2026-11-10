package com.lernspace.app.ui.planner.ocr

import java.util.Locale
import java.util.UUID

data class ParsedScheduleItem(
    val id: String = UUID.randomUUID().toString(),
    var subjectName: String,
    var subjectCode: String = "",
    var dayOfWeek: Int = 1, // 1 = Senin, ..., 7 = Minggu
    var startTime: String = "08:00",
    var endTime: String = "09:30",
    var room: String = "",
    var lecturer: String = "",
    var isSelected: Boolean = true
) {
    val dayName: String
        get() = when (dayOfWeek) {
            1 -> "Senin"
            2 -> "Selasa"
            3 -> "Rabu"
            4 -> "Kamis"
            5 -> "Jumat"
            6 -> "Sabtu"
            7 -> "Minggu"
            else -> "Senin"
        }
}

object ScheduleOcrParser {

    private val TIME_RANGE_REGEX = Regex(
        """\b([01]?\d|2[0-3])[:.]([0-5]\d)\s*(?:-|–|—|s/?d|s\.d|sampai|to)\s*([01]?\d|2[0-3])[:.]([0-5]\d)\b""",
        RegexOption.IGNORE_CASE
    )

    private val ROOM_REGEX = Regex(
        """\b(?:R\.|Ruang|Lab|Laboratorium|Gedung|Gd\.|Hall|Auditorium|Aud|Studio|Kelas)\s*([A-Za-z0-9\-./\s]{1,16})""",
        RegexOption.IGNORE_CASE
    )

    private val SUBJECT_CODE_REGEX = Regex(
        """\b([A-Z]{2,5}[-_]?\d{3,4})\b"""
    )

    private val NOISE_WORDS = setOf(
        "sks", "semester", "sem", "teori", "praktek", "praktikum", "dosen",
        "pengampu", "mata", "kuliah", "matakuliah", "mk", "kode", "no",
        "hari", "waktu", "jam", "ruang", "ruangan", "kelas", "kls", "jadwal",
        "krs", "tahun", "akademik", "program", "studi", "prodi", "gasal", "genap",
        "ajaran", "sma", "smp", "smk", "berlaku", "mulai", "tanggal", "asc", "timetables"
    )

    fun parseDayOfWeek(text: String): Int? {
        val lower = text.lowercase(Locale.ROOT).trim()
        return when {
            lower.contains("senin") || lower.contains("monday") || lower == "sen" || lower.startsWith("sen ") || lower.endsWith(" sen") -> 1
            lower.contains("selasa") || lower.contains("tuesday") || lower == "sel" || lower.startsWith("sel ") || lower.endsWith(" sel") -> 2
            lower.contains("rabu") || lower.contains("wednesday") || lower == "rab" || lower.startsWith("rab ") || lower.endsWith(" rab") -> 3
            lower.contains("kamis") || lower.contains("thursday") || lower == "kam" || lower.startsWith("kam ") || lower.endsWith(" kam") -> 4
            lower.contains("jumat") || lower.contains("jum'at") || lower.contains("friday") || lower == "jum" || lower.startsWith("jum ") || lower.endsWith(" jum") -> 5
            lower.contains("sabtu") || lower.contains("saturday") || lower == "sab" || lower.startsWith("sab ") || lower.endsWith(" sab") -> 6
            lower.contains("minggu") || lower.contains("ahad") || lower.contains("sunday") || lower == "min" || lower.startsWith("min ") || lower.endsWith(" min") -> 7
            else -> null
        }
    }

    /**
     * Parses raw plain text (e.g. copied from WhatsApp, website, or notes).
     */
    fun parseFromPlainText(rawText: String): List<ParsedScheduleItem> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val results = mutableListOf<ParsedScheduleItem>()

        var currentDay = 1

        for (line in lines) {
            val foundDay = parseDayOfWeek(line)
            if (foundDay != null && !line.contains(TIME_RANGE_REGEX)) {
                currentDay = foundDay
                continue
            }

            val timeMatch = TIME_RANGE_REGEX.find(line)
            if (timeMatch != null) {
                val lineDay = foundDay ?: currentDay
                val item = extractItemFromLine(line, timeMatch, lineDay)
                if (item != null) {
                    results.add(item)
                }
            }
        }

        return cleanParsedResults(results)
    }

    private fun extractItemFromLine(
        fullText: String,
        timeMatch: MatchResult,
        dayOfWeek: Int
    ): ParsedScheduleItem? {
        val startH = timeMatch.groupValues[1].toIntOrNull() ?: 8
        val startM = timeMatch.groupValues[2].toIntOrNull() ?: 0
        val endH = timeMatch.groupValues[3].toIntOrNull() ?: 9
        val endM = timeMatch.groupValues[4].toIntOrNull() ?: 30

        val startTime = String.format(Locale.US, "%02d:%02d", startH, startM)
        val endTime = String.format(Locale.US, "%02d:%02d", endH, endM)

        var room = ""
        val roomMatch = ROOM_REGEX.find(fullText)
        if (roomMatch != null) {
            room = roomMatch.value.trim()
        }

        var subjectCode = ""
        val codeMatch = SUBJECT_CODE_REGEX.find(fullText)
        if (codeMatch != null) {
            subjectCode = codeMatch.value.trim()
        }

        var lecturer = ""
        val lecturerMatch = Regex("""\b(?:Dosen|Pengajar|Guru|Bpk|Ibu|Dr\.|Prof\.|Ir\.)\s*[:\-]?\s*([A-Za-z0-9\s.,]+?)(?=\s*(?:R\.|Ruang|Lab|$))""", RegexOption.IGNORE_CASE).find(fullText)
            ?: Regex("""\b([A-Za-z\s]+(?:S\.Pd|M\.Pd|S\.Kom|M\.Kom|S\.T|M\.T|S\.Si|M\.Si|S\.Sos|S\.Ag|Ph\.D)\.?)\b""", RegexOption.IGNORE_CASE).find(fullText)
        if (lecturerMatch != null) {
            lecturer = (lecturerMatch.groupValues.getOrNull(1) ?: lecturerMatch.value).trim()
        }

        var cleaned = fullText
        cleaned = cleaned.replace(timeMatch.value, " ")
        if (room.isNotBlank()) cleaned = cleaned.replace(room, " ")
        if (subjectCode.isNotBlank()) cleaned = cleaned.replace(subjectCode, " ")
        if (lecturer.isNotBlank()) cleaned = cleaned.replace(lecturer, " ")

        val dayTokens = listOf("senin", "selasa", "rabu", "kamis", "jumat", "jum'at", "sabtu", "minggu", "sen", "sel", "rab", "kam", "jum", "sab", "min")
        dayTokens.forEach { token ->
            cleaned = cleaned.replace(Regex("""\b$token\b""", RegexOption.IGNORE_CASE), " ")
        }

        val words = cleaned.split(Regex("""[\s,;|/\\()\[\]]+""")).filter { w ->
            w.isNotBlank() &&
            w.length > 1 &&
            !NOISE_WORDS.contains(w.lowercase(Locale.ROOT)) &&
            !w.all { it.isDigit() }
        }

        val subjectName = if (words.isNotEmpty()) {
            words.joinToString(" ")
        } else {
            "Mata Pelajaran ${dayOfWeek}"
        }

        if (subjectName.isBlank() || subjectName.length < 2) return null

        return ParsedScheduleItem(
            subjectName = subjectName,
            subjectCode = subjectCode,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            room = room,
            lecturer = lecturer,
            isSelected = true
        )
    }

    private fun cleanParsedResults(list: List<ParsedScheduleItem>): List<ParsedScheduleItem> {
        val seen = mutableSetOf<String>()
        val distinct = mutableListOf<ParsedScheduleItem>()

        for (item in list) {
            val key = "${item.dayOfWeek}_${item.startTime}_${item.subjectName.lowercase(Locale.ROOT)}"
            if (seen.add(key)) {
                distinct.add(item)
            }
        }

        return distinct.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
    }
}
