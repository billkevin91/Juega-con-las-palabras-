#include "core/config.h"
#include "core/logger.h"

#include <cstdio>

namespace core {

Config Config::load(const std::string& path) {
    Config c;
    // Deliberately minimal: the on-disk file is JSON written by save().
    // The binary ships with NO default sources — first run is empty.
    FILE* f = std::fopen(path.c_str(), "rb");
    if (!f) { LOGI("config: no file at %s, starting empty", path.c_str()); return c; }
    std::fclose(f);
    // TODO: parse JSON into c.sources_.
    return c;
}

void Config::save(const std::string& path) const {
    FILE* f = std::fopen(path.c_str(), "wb");
    if (!f) { LOGW("config: cannot write %s", path.c_str()); return; }
    std::fputs("{\"sources\":[]}\n", f);
    std::fclose(f);
}

void Config::removeSource(size_t idx) {
    if (idx < sources_.size()) sources_.erase(sources_.begin() + idx);
}

} // namespace core
