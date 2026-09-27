package dev.ccandroid.core

import java.security.SecureRandom

public interface IdGenerator {
    public fun newId(prefix: String = ""): String
}

public class DefaultIdGenerator : IdGenerator {
    private val random = SecureRandom()
    private val crockfordBase32 = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    override fun newId(prefix: String): String {
        val timestamp = System.currentTimeMillis()
        val sb = StringBuilder()

        // 10 chars timestamp
        var time = timestamp
        for (i in 0 until 10) {
            sb.append(crockfordBase32[(time and 31).toInt()])
            time = time shr 5
        }
        sb.reverse()

        // 16 chars random
        val randomBytes = ByteArray(16)
        random.nextBytes(randomBytes)
        for (b in randomBytes) {
            sb.append(crockfordBase32[(b.toInt() and 0xFF) % 32])
        }

        return if (prefix.isEmpty()) sb.toString() else "${prefix}_${sb}"
    }
}
