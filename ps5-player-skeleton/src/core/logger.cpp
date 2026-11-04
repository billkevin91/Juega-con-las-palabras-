#include "core/logger.h"

namespace core {

Logger& Logger::get() { static Logger g; return g; }

void Logger::log(LogLevel lv, const char* fmt, ...) {
    if (static_cast<int>(lv) < static_cast<int>(level_)) return;
    static const char* tag[] = {"T","D","I","W","E"};
    std::fprintf(out_, "[%s] ", tag[static_cast<int>(lv)]);
    va_list ap;
    va_start(ap, fmt);
    std::vfprintf(out_, fmt, ap);
    va_end(ap);
    std::fputc('\n', out_);
}

} // namespace core
