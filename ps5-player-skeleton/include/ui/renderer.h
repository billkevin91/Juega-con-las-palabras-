#pragma once
#include <cstdint>
#include <string>

struct SDL_Window;
typedef void* SDL_GLContext;

namespace ui {

class Renderer {
public:
    Renderer(int w, int h);
    ~Renderer();
    Renderer(const Renderer&) = delete;
    Renderer& operator=(const Renderer&) = delete;

    void beginFrame();
    void endFrame();
    float dt() const { return dt_; }
    int width()  const { return w_; }
    int height() const { return h_; }

    void drawRect(float x, float y, float w, float h, uint32_t rgba);
    void drawText(float x, float y, const std::string& s, uint32_t rgba, float size = 24.0f);

private:
    SDL_Window* win_ = nullptr;
    SDL_GLContext gl_ = nullptr;
    int w_ = 0, h_ = 0;
    float dt_ = 0.0f;
    uint64_t lastTick_ = 0;
};

} // namespace ui
