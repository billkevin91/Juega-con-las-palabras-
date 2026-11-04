#pragma once
#include <cstdarg>
#include <cstdio>

namespace core {

enum class LogLevel { Trace, Debug, Info, Warn, Error };

class Logger {
public:
    static Logger& get();
    void setLevel(LogLevel lv) { level_ = lv; }
    void log(LogLevel lv, const char* fmt, ...);

private:
    LogLevel level_ = LogLevel::Info;
    std::FILE* out_ = stderr;
};

} // namespace core

#define LOGI(...) ::core::Logger::get().log(::core::LogLevel::Info,  __VA_ARGS__)
#define LOGW(...) ::core::Logger::get().log(::core::LogLevel::Warn,  __VA_ARGS__)
#define LOGE(...) ::core::Logger::get().log(::core::LogLevel::Error, __VA_ARGS__)
