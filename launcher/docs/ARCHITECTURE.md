# Architecture contract

Four layers (roadmap Figure 1). Layers 1-2 are this fork's code; layer 3 is upstream
hakuX, kept intact and patched only where fixes land; layer 4 is the phone, read-only.

## Launch-request flow (the only contract between UI and core)

```
UI (app/)  --LaunchRequest-->  services layer  --validated request-->  JNI (native-glue/)  -->  core (core-hakux/)
```

`LaunchRequest` = { game, GameProfile, VulkanDriver, ordered ModStack }.

- The modloader stages and checksum-verifies mods BEFORE the core starts.
- The driverloader injects the chosen Vulkan ICD BEFORE the Vulkan instance exists.
- The malicompat layer reads its toggles from the same GameProfile the UI wrote.
- The launcher never touches emulator internals directly; everything goes through the
  request object so each service is testable from a fake core.

## Rules

1. Rebase onto the newest upstream hakuX release at the start of every phase.
2. Everything this project owns lives outside the upstream source tree wherever possible,
   so upstream merges stay small.
3. No root, no unlocked bootloader: layer 4 (mali_kbase, stock kernel) is never modified.
4. GPL-3.0-or-later: every release publishes full buildable source + notices.
