#include "net/playlist_parser.h"

#include <sstream>

namespace net {

core::Result parseM3U(const std::string& text, std::vector<core::PlaylistEntry>& out) {
    std::istringstream is(text);
    std::string line, pendingTitle;
    bool header = false;
    while (std::getline(is, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        if (line.empty()) continue;
        if (!header) {
            if (line.rfind("#EXTM3U", 0) == 0) { header = true; continue; }
            return core::Result::ParseError;
        }
        if (line.rfind("#EXTINF", 0) == 0) {
            auto comma = line.find(',');
            pendingTitle = (comma == std::string::npos) ? "" : line.substr(comma + 1);
        } else if (line[0] != '#') {
            core::PlaylistEntry e;
            e.title = pendingTitle.empty() ? line : pendingTitle;
            e.url = line;
            out.push_back(std::move(e));
            pendingTitle.clear();
        }
    }
    return core::Result::Ok;
}

core::Result parseJSON(const std::string&, std::vector<core::PlaylistEntry>&) {
    // TODO: hook a small JSON parser (e.g. nlohmann/json) and map
    // { "entries": [ { "title": "...", "url": "..." } ] }.
    return core::Result::Unsupported;
}

core::Result parseXSPF(const std::string&, std::vector<core::PlaylistEntry>&) {
    // TODO: parse XSPF via a small XML reader.
    return core::Result::Unsupported;
}

core::Result parseAuto(const std::string& hint, const std::string& text,
                       std::vector<core::PlaylistEntry>& out) {
    auto has = [&](const char* s) { return hint.find(s) != std::string::npos; };
    if (has(".m3u") || has("mpegurl")) return parseM3U(text, out);
    if (has(".json") || has("json"))   return parseJSON(text, out);
    if (has(".xspf") || has("xml"))    return parseXSPF(text, out);
    if (!text.empty() && text.rfind("#EXTM3U", 0) == 0) return parseM3U(text, out);
    return core::Result::Unsupported;
}

} // namespace net
