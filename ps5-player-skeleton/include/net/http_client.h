#pragma once
#include <chrono>
#include <cstdint>
#include <functional>
#include <string>
#include <vector>

namespace net {

using Headers = std::vector<std::pair<std::string, std::string>>;

struct Response {
    long status = 0;
    std::vector<uint8_t> body;
    Headers headers;
};

class HttpClient {
public:
    HttpClient();
    ~HttpClient();
    HttpClient(const HttpClient&) = delete;
    HttpClient& operator=(const HttpClient&) = delete;

    Response get(const std::string& url,
                 const Headers& extra = {},
                 std::chrono::milliseconds timeout = std::chrono::seconds(10));

    // Streaming variant: onChunk returns false to abort.
    bool getStream(const std::string& url,
                   std::function<bool(const uint8_t*, size_t)> onChunk,
                   const Headers& extra = {});

private:
    void* handle_ = nullptr;
};

} // namespace net
