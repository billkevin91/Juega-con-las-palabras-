Generic PS5 Homebrew Media Frontend — Skeleton
==============================================

Neutral, content-agnostic media player scaffold for the open PS5 homebrew
toolchain. The binary ships with **zero** URLs, playlists, credentials, or
provider-specific logic. The end user supplies all data sources at runtime,
under their own responsibility and local environment.

Dependencies (compiled against the PS5SDK sysroot):
  - SDL2 (window, GL context, input bootstrap)
  - FFmpeg (demux + decode; build with --disable-nonfree --disable-gpl)
  - libcurl + mbedTLS (HTTP/HTTPS fetching)

Layout
------
  include/ , src/     code by layer (core, net, media, ui, input)
  toolchain/          CMake toolchain file targeting the PS5 triple
  third_party/        git-submodule the libs here
  assets/             neutral fonts/icons only

Build (example)
---------------
  cmake -S . -B build \
        -DCMAKE_TOOLCHAIN_FILE=toolchain/ps5sdk.cmake \
        -DCMAKE_BUILD_TYPE=Release
  cmake --build build -j

License
-------
MIT (see LICENSE). Third-party libraries retain their own licenses; see
NOTICE once third_party/ is populated.
