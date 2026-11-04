#pragma once
#include "core/types.h"
#include <string>
#include <vector>

namespace core {

class Config {
public:
    static Config load(const std::string& path);
    void save(const std::string& path) const;

    const std::vector<Source>& sources() const { return sources_; }
    void addSource(Source s) { sources_.push_back(std::move(s)); }
    void removeSource(size_t idx);

private:
    std::vector<Source> sources_;
};

} // namespace core
