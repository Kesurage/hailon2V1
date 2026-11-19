# core-hakux: upstream integration point

Workflow (roadmap P0):
1. Clone `rfandango/hakuX` at the newest release tag; build its core artifact.
2. Drop the AAR into `core-hakux/libs/` and enable the dependency above.
3. Keep this fork's changes as a small patch series over upstream (`patches/*.patch`)
   so every phase can start by rebasing onto the newest upstream release.
4. Preserve upstream `licenses/` in every build (GPL-3.0 obligation; see README).
