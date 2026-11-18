package dev.hakuxh2.services.modloader

/** One mod in the stack (docs/MOD_FORMAT.md). Enabled mods apply in loadOrder. */
data class ModEntry(
    val id: String,
    val name: String,
    val loadOrder: Int,
    val enabled: Boolean = false,
)

sealed interface StageResult {
    data object Ok : StageResult
    data class Rejected(val reason: String) : StageResult   // checksum mismatch etc.
}

/**
 * Phase 3: copy-on-write overlay + xdelta applier. The pristine dump is NEVER modified;
 * `disable` restores retail behavior. Verify SHA-256 of every touched base map before
 * staging, and fail safe to an unmodded launch on any mismatch.
 */
interface ModLoader {
    fun stage(mods: List<ModEntry>, gameImagePath: String): StageResult
    fun dryRun(mods: List<ModEntry>, gameImagePath: String): StageResult
}
