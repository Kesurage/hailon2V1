# No-code flicker triage (run in order, record each result)

1. Update hakuX to the newest official release (1.3.0+). Two Halo 2 graphics bugs and one
   Mali-G57 bug are already fixed upstream.
2. Internal resolution scale to 1x, retest 20 minutes of campaign. If flicker stops, the
   cause is the scaling path; FSR becomes the only sanctioned upscale route.
3. Developer options -> "Disable HW overlays" (GPU composition), retest.
4. Display settings: disable adaptive refresh, lock 60 Hz, retest.
5. Disable auto-brightness for the test session.
6. Battery: set the emulator to unrestricted.
7. If flicker persists at 1x with overlays disabled: capture a 2-minute clip + bug-report
   bundle. Driver-layer vs compositor-layer flicker is distinguishable from the pair.
8. Optional (Phase 4+): try the panvk-kbase alternate renderer — a different driver is
   itself a diagnostic for blob-specific flicker.
