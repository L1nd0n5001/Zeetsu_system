package com.example.crypto

object RainbowDictionary {

    val TOP_WORDS: List<String> = listOf(
        // Common passwords & top wordlists
        "123456", "password", "12345678", "qwerty", "123456789", "12345", "1234",
        "111111", "1234567", "dragon", "123123", "baseball", "football", "welcome",
        "sunshine", "monkey", "charlie", "donald", "shadow", "master", "michael",
        "superman", "696969", "batman", "trustno1", "admin", "root", "toor", "guest",
        "pass", "secret", "default", "system", "zeetsu", "zeetsu system", "login",
        "database", "matrix", "cyber", "access", "terminal", "security", "android",
        "crypto", "hacker", "algorithm", "network", "server", "hash", "md5", "sha256",
        "hello", "world", "hello world", "test", "demo", "sample", "qwertyuiop",
        "asdfghjkl", "zxcvbnm", "letmein", "princess", "starwars", "solo", "jedi",
        "computer", "internet", "iloveyou", "love", "baby", "angel", "sweet",
        "family", "forever", "god", "jesus", "money", "freedom", "cookie", "coffee",
        "pizza", "orange", "apple", "banana", "galaxy", "universe", "phantom",
        "vortex", "omega", "alpha", "delta", "echo", "foxtrot", "bravo", "victor",
        "000000", "7777777", "999999", "888888", "123321", "654321", "112233",
        "223344", "pass123", "admin123", "root123", "welcome1", "summer", "winter",
        "spring", "autumn", "november", "december", "january", "password123",
        "p@ssword", "P@ssw0rd", "pass@123", "admin@123", "zeetsu@2026", "brasil",
        "portugal", "futebol", "flamengo", "corinthians", "palmeiras", "senha",
        "senha123", "123mudar", "trocar123", "mudar123", "admin2024", "admin2025",
        "admin2026", "root2026", "support", "service", "operator", "user", "manager",
        "supervisor", "engineer", "developer", "tester", "moderator", "superuser"
    )

    /**
     * Generates a sequence of test candidates (common words, pin numbers, short tokens).
     */
    fun getCandidateSequence(): Sequence<String> = sequence {
        // Yield Top Words
        for (w in TOP_WORDS) {
            yield(w)
            yield(w.lowercase())
            yield(w.uppercase())
            yield(w.replaceFirstChar { it.uppercase() })
        }

        // Yield common numeric PINs 0000 to 9999
        for (i in 0..9999) {
            yield(i.toString())
            yield(String.format("%04d", i))
        }

        // Yield 5-digit and 6-digit popular sequences
        for (i in 10000..10100) yield(i.toString())
        for (i in 123450..123460) yield(i.toString())
        for (i in 111110..111120) yield(i.toString())
        for (i in 999990..999999) yield(i.toString())
        for (i in 0..999) {
            yield(String.format("%06d", i))
        }

        // 1-2 char alphanumeric tokens
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        for (c1 in chars) {
            yield(c1.toString())
            for (c2 in chars) {
                yield("$c1$c2")
            }
        }
    }
}
