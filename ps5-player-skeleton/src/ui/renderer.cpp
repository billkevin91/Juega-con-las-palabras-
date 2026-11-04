#include "ui/renderer.h"
#include "core/logger.h"

#include <SDL2/SDL.h>

namespace ui {

Renderer::Renderer(int w, int h) : w_(w), h_(h) {
    SDL_Init(SDL_INIT_VIDEO);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 5);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE);
    win_ = SDL_CreateWindow("ps5_player", 0, 0, w, h,
                            SDL_WINDOW_OPENGL | SDL_WINDOW_FULLSCREEN);
    if (!win_) { LOGE("renderer: SDL_CreateWindow failed"); return; }
    gl_ = SDL_GL_CreateContext(win_);
    SDL_GL_SetSwapInterval(1);
    lastTick_ = SDL_GetPerformanceCounter();
}
Renderer::~Renderer() {
    if (gl_)  SDL_GL_DeleteContext(gl_);
    if (win_) SDL_DestroyWindow(win_);
    SDL_Quit();
}

void Renderer::beginFrame() {
    uint64_t now = SDL_GetPerformanceCounter();
    dt_ = static_cast<float>(now - lastTick_) /
          static_cast<float>(SDL_GetPerformanceFrequency());
    lastTick_ = now;
    // TODO: glClear via GLAD/glad2; omitted to keep the skeleton tiny.
}
void Renderer::endFrame() {
    if (win_) SDL_GL_SwapWindow(win_);
}

void Renderer::drawRect(float, float, float, float, uint32_t) {
    // TODO: batch into a textured-quad vertex stream.
}
void Renderer::drawText(float, float, const std::string&, uint32_t, float) {
    // TODO: stb_truetype atlas + quad emission.
}

} // namespace ui
