import java.util.*
import kotlin.math.min

class TeacherNormalizer {
    private val SIMILARITY_THRESHOLD = 0.90
    private val TEACHER_MAP = mutableMapOf<String, Teacher>()

    data class Teacher(
        var canonicalName: String,
        var phone: String = "",
        val variations: MutableSet<String> = mutableSetOf()
    )

    fun normalizeName(name: String): String {
        if (name.isBlank()) return ""
        
        return name.lowercase()
            .replace(Regex("(sir|miss|mr|ms|mrs|sr|dr)\\.?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[^a-z\\s-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(" ")
            .filter { it.length > 1 }
            .sorted()
            .joinToString(" ")
    }

    fun simplifiedMetaphone(str: String): String {
        if (str.isBlank()) return ""
        
        return str.lowercase()
            .replace(Regex("[aeiou]"), "a")
            .replace(Regex("[^a-z]"), "")
            .replace(Regex("(.)\\1+"), "$1")
            .take(8)
    }

    fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val matrix = Array(b.length + 1) { IntArray(a.length + 1) }

        for (i in 0..a.length) matrix[0][i] = i
        for (j in 0..b.length) matrix[j][0] = j

        for (j in 1..b.length) {
            for (i in 1..a.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                matrix[j][i] = min(
                    min(matrix[j][i - 1] + 1, matrix[j - 1][i] + 1),
                    matrix[j - 1][i - 1] + cost
                )
            }
        }

        return matrix[b.length][a.length]
    }

    fun nameSimilarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val aMeta = simplifiedMetaphone(a)
        val bMeta = simplifiedMetaphone(b)
        val distance = levenshtein(aMeta, bMeta)
        var similarity = 1.0 - distance.toDouble() / maxOf(aMeta.length, bMeta.length, 1)

        // Check for substring matches
        if (a.contains(b, ignoreCase = true) || b.contains(a, ignoreCase = true)) {
            similarity = maxOf(similarity, 0.95)
        }

        // Check for common name parts
        val aParts = a.split(" ").toSet()
        val bParts = b.split(" ").toSet()
        val commonParts = aParts.intersect(bParts)
        if (commonParts.isNotEmpty()) {
            similarity = maxOf(similarity, 0.85 + (0.05 * commonParts.size))
        }

        return similarity
    }

    fun generateVariations(name: String): List<String> {
        val variations = mutableSetOf<String>()
        variations.add(name)
        variations.add(name.lowercase())

        // Without honorific
        val withoutHonorific = name.replace(Regex("^(sir|miss|mr|ms|mrs|sr|dr)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
        variations.add(withoutHonorific)
        variations.add(withoutHonorific.lowercase())

        // With swapped honorific
        when {
            name.startsWith("Sir", ignoreCase = true) -> {
                variations.add(name.replace(Regex("^Sir", RegexOption.IGNORE_CASE), "Miss"))
            }
            name.startsWith("Miss", ignoreCase = true) -> {
                variations.add(name.replace(Regex("^Miss", RegexOption.IGNORE_CASE), "Sir"))
            }
        }

        return variations.toList()
    }

    fun registerTeacher(rawName: String, phone: String = ""): Boolean {
        if (rawName.isBlank() || rawName.equals("empty", ignoreCase = true) || rawName.trim().length < 2) {
            return false
        }

        val normalized = normalizeName(rawName)
        if (normalized.isEmpty()) return false

        // Check exact matches
        TEACHER_MAP[normalized]?.let { existing ->
            existing.variations.add(rawName)
            if (phone.isNotEmpty() && existing.phone.isEmpty()) {
                existing.phone = phone
            }
            return true
        }

        // Check similar names
        for ((_, teacher) in TEACHER_MAP) {
            val teacherNormalized = normalizeName(teacher.canonicalName)
            val similarity = nameSimilarity(normalized, teacherNormalized)
            if (similarity >= SIMILARITY_THRESHOLD) {
                teacher.variations.add(rawName)
                if (phone.isNotEmpty() && teacher.phone.isEmpty()) {
                    teacher.phone = phone
                }
                return true
            }
        }

        // Add new teacher
        TEACHER_MAP[normalized] = Teacher(
            canonicalName = rawName.trim().replace(Regex("\\s+"), " "),
            phone = phone,
            variations = mutableSetOf(rawName)
        )

        return true
    }

    // Additional helper function to access registered teachers
    fun getTeachers(): List<Teacher> = TEACHER_MAP.values.toList()
}