#include "net/http_client.h"
#include "core/logger.h"

#include <curl/curl.h>

namespace net {

static size_t writeVec(void* ptr, size_t sz, size_t n, void* ud) {
    auto* v = static_cast<std::vector<uint8_t>*>(ud);
    size_t bytes = sz * n;
    v->insert(v->end(), static_cast<uint8_t*>(ptr), static_cast<uint8_t*>(ptr) + bytes);
    return bytes;
}

HttpClient::HttpClient() {
    curl_global_init(CURL_GLOBAL_DEFAULT);
    handle_ = curl_easy_init();
}
HttpClient::~HttpClient() {
    if (handle_) curl_easy_cleanup(static_cast<CURL*>(handle_));
    curl_global_cleanup();
}

Response HttpClient::get(const std::string& url, const Headers& extra,
                         std::chrono::milliseconds timeout) {
    Response r;
    CURL* h = static_cast<CURL*>(handle_);
    if (!h) return r;
    curl_easy_reset(h);
    curl_easy_setopt(h, CURLOPT_URL, url.c_str());
    curl_easy_setopt(h, CURLOPT_WRITEFUNCTION, writeVec);
    curl_easy_setopt(h, CURLOPT_WRITEDATA, &r.body);
    curl_easy_setopt(h, CURLOPT_FOLLOWLOCATION, 1L);
    curl_easy_setopt(h, CURLOPT_TIMEOUT_MS, static_cast<long>(timeout.count()));

    struct curl_slist* hdrs = nullptr;
    for (auto& kv : extra) {
        std::string line = kv.first + ": " + kv.second;
        hdrs = curl_slist_append(hdrs, line.c_str());
    }
    if (hdrs) curl_easy_setopt(h, CURLOPT_HTTPHEADER, hdrs);

    CURLcode rc = curl_easy_perform(h);
    if (rc != CURLE_OK) LOGW("http get: %s", curl_easy_strerror(rc));
    curl_easy_getinfo(h, CURLINFO_RESPONSE_CODE, &r.status);
    if (hdrs) curl_slist_free_all(hdrs);
    return r;
}

bool HttpClient::getStream(const std::string& url,
                           std::function<bool(const uint8_t*, size_t)> onChunk,
                           const Headers& extra) {
    CURL* h = static_cast<CURL*>(handle_);
    if (!h) return false;
    curl_easy_reset(h);

    struct Ctx { std::function<bool(const uint8_t*, size_t)>* cb; bool abort; };
    Ctx ctx{&onChunk, false};

    auto trampoline = +[](void* p, size_t sz, size_t n, void* ud) -> size_t {
        auto* c = static_cast<Ctx*>(ud);
        if (c->abort) return 0;
        if (!(*c->cb)(static_cast<uint8_t*>(p), sz * n)) { c->abort = true; return 0; }
        return sz * n;
    };
    curl_easy_setopt(h, CURLOPT_URL, url.c_str());
    curl_easy_setopt(h, CURLOPT_WRITEFUNCTION, trampoline);
    curl_easy_setopt(h, CURLOPT_WRITEDATA, &ctx);
    curl_easy_setopt(h, CURLOPT_FOLLOWLOCATION, 1L);

    struct curl_slist* hdrs = nullptr;
    for (auto& kv : extra) {
        std::string line = kv.first + ": " + kv.second;
        hdrs = curl_slist_append(hdrs, line.c_str());
    }
    if (hdrs) curl_easy_setopt(h, CURLOPT_HTTPHEADER, hdrs);

    CURLcode rc = curl_easy_perform(h);
    if (hdrs) curl_slist_free_all(hdrs);
    return rc == CURLE_OK;
}

} // namespace net
