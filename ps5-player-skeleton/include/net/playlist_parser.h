#pragma once
#include "core/types.h"
#include <string>

namespace net {

// Parse open, user-supplied playlist formats. No data is embedded in the
// binary; the caller provides the raw text.
core::Result parseM3U(const std::string& text, std::vector<core::PlaylistEntry>& out);
core::Result parseJSON(const std::string& text, std::vector<core::PlaylistEntry>& out);
core::Result parseXSPF(const std::string& text, std::vector<core::PlaylistEntry>& out);

// Dispatch by extension/content-type hint.
core::Result parseAuto(const std::string& hint,
                       const std::string& text,
                       std::vector<core::PlaylistEntry>& out);

} // namespace net
