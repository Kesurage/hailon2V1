#!/usr/bin/env bash
# Fetches the native core's third-party sources (NC_DEPS) at the revisions
# android/README.md pins, into the layout native/cmake expects. Run from the
# repository root:
#
#   bash tools/ci/fetch-nc-deps.sh <nc-deps-dir>
#
# Idempotent: a dependency already fetched (marker file) is skipped, so it can
# run again on a warm cache. All sources are public; nothing is patched except
# adrenotools, whose kgsl-context-priority patch (tools/native/) hakuX pins.
set -euo pipefail

DEST=${1:?usage: fetch-nc-deps.sh <nc-deps-dir>}
mkdir -p "$DEST"

# clone_tag <url> <tag> <dest>: a tag (or branch) reachable by --depth 1.
clone_tag() {
  local url=$1 ref=$2 dest=$3
  if [ -f "$dest/.fetched" ]; then
    echo "fetch-nc-deps: $dest already fetched"
    return
  fi
  rm -rf "$dest"
  git clone --depth 1 --branch "$ref" "$url" "$dest"
  touch "$dest/.fetched"
}

# clone_sha <url> <full-sha> <dest>: a commit (fetched by its full sha).
clone_sha() {
  local url=$1 sha=$2 dest=$3
  if [ -f "$dest/.fetched" ]; then
    echo "fetch-nc-deps: $dest already fetched"
    return
  fi
  rm -rf "$dest"
  mkdir -p "$dest"
  git -C "$dest" init -q
  git -C "$dest" remote add origin "$url"
  git -C "$dest" fetch --depth 1 origin "$sha"
  git -C "$dest" checkout -q FETCH_HEAD
  touch "$dest/.fetched"
}

echo "== NC_DEPS -> $DEST =="

# volk 1.4.304 (the Vulkan loader shim)
clone_tag https://github.com/zeux/volk 1.4.304 "$DEST/volk"

# glslang 16.1.0 (the shader compiler)
clone_tag https://github.com/KhronosGroup/glslang 16.1.0 "$DEST/glslang"

# spirv_reflect vulkan-sdk-1.4.328.0
clone_tag https://github.com/KhronosGroup/SPIRV-Reflect vulkan-sdk-1.4.328.0 "$DEST/spirv_reflect"

# vma v3.3.0 (header-only allocator; the repo moved, it is no longer
# GPUOpen-LibrariesAndSDKs/Vulkan-Memory-Allocator)
clone_tag https://github.com/GPUOpen-LibrariesAndSDKs/VulkanMemoryAllocator v3.3.0 "$DEST/vma"

# SDL3 release-3.4.18
clone_tag https://github.com/libsdl-org/SDL release-3.4.18 "$DEST/SDL3"

# adrenotools 8fae8ce (custom Vulkan driver loading), its linkernsbypass
# submodule, and hakuX's kgsl context priority patch
if [ ! -f "$DEST/adrenotools/.fetched" ]; then
  rm -rf "$DEST/adrenotools"
  clone_sha https://github.com/bylaws/libadrenotools \
    8fae8ce254dfc1344527e05301e43f37dea2df80 "$DEST/adrenotools"
  git -C "$DEST/adrenotools" submodule update --init
  git -C "$DEST/adrenotools" apply --check "$(pwd)/tools/native/adrenotools-kgsl-context-priority.patch"
  git -C "$DEST/adrenotools" apply "$(pwd)/tools/native/adrenotools-kgsl-context-priority.patch"
  touch "$DEST/adrenotools/.fetched"
fi

# nv2a_vsh_cpu 561fe80 (the vertex shader translator; the layout native/cmake
# reads is root/subprojects/nv2a_vsh_cpu, and "root" is an include directory)
mkdir -p "$DEST/root/subprojects"
if [ ! -f "$DEST/root/subprojects/nv2a_vsh_cpu/.fetched" ]; then
  clone_sha https://github.com/xemu-project/nv2a_vsh_cpu \
    561fe80da57a881f89000256b459440c0178a7ce "$DEST/root/subprojects/nv2a_vsh_cpu"
fi

# libslirp 4.7.0-era master, 26be815b (the revision android/README.md pins —
# the v4.7.0 tag itself predates slirp_ssize_t, which native/net/nat.c uses)
clone_sha https://gitlab.freedesktop.org/slirp/libslirp.git \
  26be815b86e8d49add8c9a8b320239b9594ff03d "$DEST/libslirp"

echo "fetch-nc-deps: done"
du -sh "$DEST"/* | sed 's|^|  |'
