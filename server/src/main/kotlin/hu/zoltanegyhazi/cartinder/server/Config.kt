package hu.zoltanegyhazi.cartinder.server

import java.io.File

data class Config(
    val port: Int = 8080,
    val databaseUrl: String = "jdbc:sqlite:data/cartinder.db",
    val databaseUser: String = "",
    val databasePassword: String = "",
    val jwtSecret: String = DEV_SECRET,
    val uploadDir: File = File("data/uploads"),
    val seedDemoData: Boolean = true,
    /** Demó eladók válaszideje; tesztekben nulla. */
    val botDelayMs: LongRange = 900L..2200L,
) {
    companion object {
        const val DEV_SECRET = "dev-secret-change-me"

        fun fromEnv(env: Map<String, String> = System.getenv()) = Config(
            port = env["PORT"]?.toInt() ?: 8080,
            databaseUrl = env["DATABASE_URL"] ?: "jdbc:sqlite:data/cartinder.db",
            databaseUser = env["DATABASE_USER"].orEmpty(),
            databasePassword = env["DATABASE_PASSWORD"].orEmpty(),
            jwtSecret = env["JWT_SECRET"] ?: DEV_SECRET,
            uploadDir = File(env["UPLOAD_DIR"] ?: "data/uploads"),
            seedDemoData = env["SEED_DEMO_DATA"]?.toBooleanStrictOrNull() ?: true,
        )
    }
}
