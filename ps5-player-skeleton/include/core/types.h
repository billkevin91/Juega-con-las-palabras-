#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace core {

struct PlaylistEntry {
    std::string title;
    std::string url;
    std::string thumbnail_url;
    std::vector<std::pair<std::string, std::string>> attrs;
};

struct Source {
    std::string name;
    std::string location;
    std::vector<PlaylistEntry> entries;
};

enum class Result { Ok, NotFound, NetworkError, ParseError, Unsupported };

} // namespace core
